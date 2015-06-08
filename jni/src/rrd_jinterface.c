/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2010-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is Copyright (C) 2002-2015 The OpenNMS Group, Inc.  All rights
 * reserved.  OpenNMS(R) is a derivative work, containing both original code,
 * included code and modified code that was published under the GNU General
 * Public License.  Copyrights for modified and included code are below.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License with the Classpath
 * Exception; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
#include "config.h"

#ifdef HAVE_UNISTD_H
#include <unistd.h>
#endif

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#ifdef HAVE_STDLIB_H
#include <stdlib.h>
#endif

#ifdef HAVE_MALLOC_H
#include <malloc.h>
#endif

#include <limits.h>

#ifdef HAVE_GETOPT_H
#include <getopt.h>
#endif

#include <jni.h>
#include <rrd.h>
#if 0
#pragma export on
#endif
#include "jrrd2_java_interface.h"
#if 0
#pragma export reset
#endif

// The signature can be determined with: javap -classpath jrrd2.jar -s -p org.opennms.netmgt.rrd.rrdtool.FetchResults
const char* FETCH_RESULTS_CONSTRUCTOR_METHOD_ID = "(JJJ[Ljava/lang/String;[[D)V";

typedef struct {
	jclass jniRrdException,
			outOfMemoryError,
			string,
			doubleArray,
			fetchResults;
} Classes;

int findClasses(JNIEnv *env, Classes* classes) {
	classes->jniRrdException = (*env)->FindClass(env, "org/opennms/netmgt/rrd/jrrd2/JniRrdException");
	if(classes->jniRrdException == NULL || (*env)->ExceptionOccurred(env) != NULL) {
		return -1;
	}

	classes->outOfMemoryError = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
	if(classes->outOfMemoryError == NULL || (*env)->ExceptionOccurred(env) != NULL) {
		return -1;
	}

	classes->string = (*env)->FindClass(env, "java/lang/String");
	if(classes->string == NULL || (*env)->ExceptionOccurred(env) != NULL) {
		return -1;
	}

	classes->doubleArray = (*env)->FindClass(env, "[D");
	if(classes->doubleArray == NULL || (*env)->ExceptionOccurred(env) != NULL) {
		return -1;
	}

	classes->fetchResults = (*env)->FindClass(env, "org/opennms/netmgt/rrd/jrrd2/FetchResults");
	if(classes->fetchResults == NULL || (*env)->ExceptionOccurred(env) != NULL) {
		return -1;
	}

	return 0;
}

static inline time_t jlong_to_time_t(jlong timestamp) {
	// WARNING: This may cause problems on some architectures.
	// Failures should be detected by the unit test suite.
	return (time_t)timestamp;
}

static inline void release_strings(JNIEnv *env, Classes *classes, jobjectArray array, const char ** strings, int size) {
	int i;
	// Release the elements
	for (i = 0; i < size; i++) {
		if (strings[i] != NULL) {
			jstring string = (jstring)(*env)->GetObjectArrayElement(env, array, i);
			(*env)->ReleaseStringUTFChars(env, string, strings[i]);
		}
	}
	// Release the array
	free(strings);
}

static inline const char ** jstrings_to_strings(JNIEnv *env, Classes *classes, jobjectArray array, int *size) {
	(*size) = (*env)->GetArrayLength(env, array);
	const char **strings = (const char **)malloc((*size)*sizeof(const char *));
	if (strings == NULL) {
		(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for array");
		return NULL;
	}
	memset(strings, 0, (*size)*sizeof(const char *));

	int i, j;
	for (i = 0; i < (*size); i++) {
		jstring string = (jstring)(*env)->GetObjectArrayElement(env, array, i);
		strings[i] = (*env)->GetStringUTFChars(env, string, 0);
		if (strings[i] == NULL) {
			release_strings(env, classes, array, strings, (*size));
			(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for string");
			return NULL;
		}
	}

	return strings;
}

static inline jobjectArray strings_to_jstrings(JNIEnv *env, Classes *classes, char **strings, int size) {
	jobjectArray array = (*env)->NewObjectArray(env, size, classes->string, NULL);
	if (array == NULL) {
		(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for string array");
		return NULL;
	}

	int i;
	for(i = 0; i < size; i++) {
		jstring utf_str = (*env)->NewStringUTF(env, strings[i]);
		if (utf_str == NULL) {
			// Since we're invoked in the context of a JVM thread, previous allocated jstrings
			// and will be automatically cleaned
			(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for string reference");
			return NULL;
		}
		(*env)->SetObjectArrayElement(env, array, i, utf_str);
	}

	return array;
}

static inline jobjectArray rrd_values_to_matrix(JNIEnv *env, Classes *classes, rrd_value_t *values, int columns, int rows) {
	jobjectArray matrix = (*env)->NewObjectArray(env, columns, classes->doubleArray, NULL);
	if (matrix == NULL) {
		(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for double[] array");
		return NULL;
	}

	int i, j;
	for (i = 0; i < columns; i++) {
		jdoubleArray column_store = (*env)->NewDoubleArray(env, rows);
		if (column_store == NULL) {
			(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for double array");
			return NULL;
		}


		jdouble *column = (jdouble *) malloc(rows * sizeof(jdouble));
		if (column == NULL) {
			(*env)->ThrowNew(env, classes->outOfMemoryError, "failed to allocate memory for the column values");
			return NULL;
		}

		for (j = 0; j < rows; j++) {
			column[j] = *(values + i + (j*columns));
		}

		(*env)->SetDoubleArrayRegion(env, column_store, 0, rows, column);

		(*env)->SetObjectArrayElement(env, matrix, i, column_store);
	}

	return matrix;
}

/**
*	rrd_context_t *rrd_get_context(void);
*/
JNIEXPORT void JNICALL Java_org_opennms_netmgt_rrd_jrrd2_Interface_rrd_1get_1context
		(JNIEnv *env, jclass clazz) {
	// Every thread should call this before its first call to any librrd_th functions
	rrd_get_context();
}

/**
*   int rrd_create_r(
	const char *filename,
	unsigned long pdp_step,
	time_t last_up,
	int argc,
	const char **argv);
*/
JNIEXPORT void JNICALL Java_org_opennms_netmgt_rrd_jrrd2_Interface_rrd_1create_1r
		(JNIEnv *env, jclass clazz, jstring filename, jlong pdp_step, jlong last_up, jobjectArray argv) {

	// Grab references to the classes we may need
	Classes classes;
	if (findClasses(env, &classes) == -1) {
		return; // Exception already thrown
	}

	// Input validation
	if (filename == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "filename cannot be null.");
		return;
	}

	if (argv == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "argv cannot be null.");
		return;
	}

	// Java -> C type conversions
	const char *n_filename = (*env)->GetStringUTFChars(env, filename, 0);
	if (n_filename == NULL) {
		return; // OutOfMemoryError already thrown
	}

	if (pdp_step < (jlong) LONG_MIN || pdp_step > (jlong) LONG_MAX) {
		(*env)->ReleaseStringUTFChars(env, filename, n_filename);
		(*env)->ThrowNew(env, classes.jniRrdException, "pdp_step out of bounds.");
		return;
	}
	long n_pdp_step = pdp_step;

	time_t n_last_up = jlong_to_time_t(last_up);

	int n_argc;
	const char **n_argv = jstrings_to_strings(env, &classes, argv, &n_argc);
	if (n_argv == NULL) {
		(*env)->ReleaseStringUTFChars(env, filename, n_filename);
		return; // OutOfMemoryError already thrown
	}

	// Make sure we don't fail because of some earlier error
	rrd_clear_error();

	// Make the actual call
	int result = rrd_create_r(n_filename, n_pdp_step, n_last_up, n_argc, n_argv);

	// Release allocated resources
	(*env)->ReleaseStringUTFChars(env, filename, n_filename);

	release_strings(env, &classes, argv, n_argv, n_argc);

	// Process the results
	if (result == -1) {
		if (rrd_test_error()) {
			(*env)->ThrowNew(env, classes.jniRrdException, rrd_get_error());
			rrd_clear_error();
		} else {
			(*env)->ThrowNew(env, classes.jniRrdException, "rrd_create_r() failed, but no error code was set.");
		}
	}
}

/**
* int rrd_update_r(
	const char *filename,
	const char *_template,
	int argc,
	const char **argv);
*/
JNIEXPORT void JNICALL Java_org_opennms_netmgt_rrd_jrrd2_Interface_rrd_1update_1r
		(JNIEnv *env, jclass clazz, jstring filename, jstring template, jobjectArray argv) {

	// Grab references to the classes we may need
	Classes classes;
	if (findClasses(env, &classes) == -1) {
		return; // Exception already thrown
	}

	// Input validation
	if (filename == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "filename cannot be null.");
		return;
	}

	if (argv == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "argv cannot be null.");
		return;
	}

	// Java -> C type conversions
	const char *n_filename = (*env)->GetStringUTFChars(env, filename, 0);
	if (n_filename == NULL) {
		return; // OutOfMemoryError already thrown
	}

	const char *n_template = NULL;
	if (template != NULL) {
		n_template = (*env)->GetStringUTFChars(env, template, 0);
		if (n_template == NULL) {
			(*env)->ReleaseStringUTFChars(env, filename, n_filename);
			return; // OutOfMemoryError already thrown
		}
	}

	int n_argc;
	const char **n_argv = jstrings_to_strings(env, &classes, argv, &n_argc);
	if (n_argv == NULL) {
		(*env)->ReleaseStringUTFChars(env, filename, n_filename);
		if (template != NULL) {
			(*env)->ReleaseStringUTFChars(env, template, n_template);
		}
		return; // OutOfMemoryError already thrown
	}

	// Make sure we don't fail because of some earlier error
	rrd_clear_error();

	// Make the actual call
	int result = rrd_update_r(n_filename, n_template, n_argc, n_argv);

	// Release allocated resources
	(*env)->ReleaseStringUTFChars(env, filename, n_filename);

	if (n_template != NULL) {
		(*env)->ReleaseStringUTFChars(env, template, n_template);
	}

	release_strings(env, &classes, argv, n_argv, n_argc);

	// Process the results
	if (result == -1) {
		if (rrd_test_error()) {
			(*env)->ThrowNew(env, classes.jniRrdException, rrd_get_error());
			rrd_clear_error();
		} else {
			(*env)->ThrowNew(env, classes.jniRrdException, "rrd_update_r() failed, but no error code was set.");
		}
	}
}

/**
*   int rrd_fetch_r (
		const char *filename,
		const char *cf,
		time_t *start,
		time_t *end,
		unsigned long *step,
		unsigned long *ds_cnt,
		char ***ds_namv,
		rrd_value_t **data);
*/
JNIEXPORT jobject JNICALL Java_org_opennms_netmgt_rrd_jrrd2_Interface_rrd_1fetch_1r
		(JNIEnv *env, jclass clazz, jstring filename, jstring cf, jlong start, jlong end, jlong step) {

	// Grab references to the classes we may need
	Classes classes;
	if (findClasses(env, &classes) == -1) {
		return NULL; // Exception already thrown
	}

	// Grab a reference to the results constructor
	jmethodID constructor = (*env)->GetMethodID(env, classes.fetchResults, "<init>", FETCH_RESULTS_CONSTRUCTOR_METHOD_ID);
	if (constructor == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "no valid constructor found.");
		return NULL;
	}

	// Input validation
	if (filename == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "filename cannot be null.");
		return NULL;
	}

	if (cf == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "cf cannot be null.");
		return NULL;
	}

	// Java -> C type conversions
	const char *n_filename = (*env)->GetStringUTFChars(env, filename, 0);
	if (n_filename == NULL) {
		return NULL; // OutOfMemoryError already thrown
	}

	const char *n_cf = (*env)->GetStringUTFChars(env, cf, 0);
	if (n_filename == NULL) {
		(*env)->ReleaseStringUTFChars(env, filename, n_filename);
		return NULL; // OutOfMemoryError already thrown
	}

	time_t n_start = jlong_to_time_t(start);

	time_t n_end = jlong_to_time_t(end);

	unsigned long n_step = (unsigned long)step;

	// Make sure we don't fail because of some earlier error
	rrd_clear_error();

	// Make the actual call
	int i;
	unsigned long n_ds_cnt;
	char **n_ds_namv;
	rrd_value_t *n_data;
	int result = rrd_fetch_r(n_filename, n_cf, &n_start, &n_end, &n_step, &n_ds_cnt, &n_ds_namv, &n_data);

	// Release allocated resources
	(*env)->ReleaseStringUTFChars(env, filename, n_filename);

	(*env)->ReleaseStringUTFChars(env, cf, n_cf);

	if (result == -1) {
		if (rrd_test_error()) {
			(*env)->ThrowNew(env, classes.jniRrdException, rrd_get_error());
			rrd_clear_error();
		} else {
			(*env)->ThrowNew(env, classes.jniRrdException, "rrd_update_r() failed, but no error code was set.");
		}

		return NULL;
	}

	// Success!
	jobject results = NULL;

	// Determine the number of rows and columns we need
	int col_cnt = (int)n_ds_cnt;
	unsigned long row_cnt = (n_end - n_start) / n_step;

	// Gather the legends
	jobjectArray results_legends = strings_to_jstrings(env, &classes, n_ds_namv, col_cnt);
	if (results_legends == NULL) {
		(*env)->ThrowNew(env, classes.outOfMemoryError, "failed to allocate memory for string array");
		goto theend;
	}

	// Gather the values
	jobjectArray result_columns = rrd_values_to_matrix(env, &classes, n_data, col_cnt, (int)row_cnt);
	if (result_columns == NULL) {
		(*env)->ThrowNew(env, classes.outOfMemoryError, "failed to allocate memory for result matrix");
		goto theend;
	}

	results = (*env)->NewObject(env, classes.fetchResults, constructor, (jlong) n_start + (jlong) n_step, (jlong) n_end, (jlong) n_step, results_legends, result_columns);

	theend:
	// Free up RRD allocations
	for (i=0;i<col_cnt;i++)
		free(n_ds_namv[i]);
	free(n_ds_namv);
	free (n_data);

	return results;
}

/**
* int rrd_xport(
	int argc,
	char **argv,
	int UNUSED(*xsize),
	time_t *start,
	time_t *end,
	unsigned long *step,
	unsigned long *col_cnt
	char ***legend_v);
*/
JNIEXPORT jobject JNICALL Java_org_opennms_netmgt_rrd_jrrd2_Interface_rrd_1xport
		(JNIEnv *env, jclass class, jobjectArray argv) {

	// Grab references to the classes we may need
	Classes classes;
	if (findClasses(env, &classes) == -1) {
		return NULL; // Exception already thrown
	}

	// Grab a reference to the results constructor
	jmethodID constructor = (*env)->GetMethodID(env, classes.fetchResults, "<init>", FETCH_RESULTS_CONSTRUCTOR_METHOD_ID);
	if (constructor == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "no valid constructor found.");
		return NULL;
	}

	// Input validation
	if (argv == NULL) {
		(*env)->ThrowNew(env, classes.jniRrdException, "argv cannot be null.");
		return NULL;
	}

	int n_argc;
	const char **n_argv = jstrings_to_strings(env, &classes, argv, &n_argc);
	if (n_argv == NULL) {
		return NULL; // OutOfMemoryError already thrown
	}

	// Make sure we don't fail because of some earlier error
	rrd_clear_error();

	// Make the actual call
	int i;
	time_t n_start, n_end;
	unsigned long n_step, n_col_cnt;
	rrd_value_t *n_data;
	char **n_legend_v;
	int n_xsize;

	int result = rrd_xport(n_argc, (char**)n_argv, &n_xsize, &n_start, &n_end, &n_step, &n_col_cnt, &n_legend_v, &n_data);

	// Release allocated resources
	release_strings(env, &classes, argv, n_argv, n_argc);

	// Handle the results
	if (result == -1) {
		if (rrd_test_error()) {
			(*env)->ThrowNew(env, classes.jniRrdException, rrd_get_error());
			rrd_clear_error();
		} else {
			(*env)->ThrowNew(env, classes.jniRrdException, "rrd_xport() failed, but no error code was set.");
		}
		return NULL;
	}

	// Success!
	jobject results = NULL;

	// Determine the number of rows and columns we need
	int col_cnt = (int)n_col_cnt;
	unsigned long row_cnt = (n_end - n_start) / n_step;

	// Build the list of legends
	jobjectArray results_legends = strings_to_jstrings(env, &classes, n_legend_v, col_cnt);
	if (results_legends == NULL) {
		(*env)->ThrowNew(env, classes.outOfMemoryError, "failed to allocate memory for string array");
		goto theend;
	}

	// Build the list of values
	jobjectArray result_columns = rrd_values_to_matrix(env, &classes, n_data, col_cnt, row_cnt);
	if (result_columns == NULL) {
		(*env)->ThrowNew(env, classes.outOfMemoryError, "failed to allocate memory for result matrix");
		goto theend;
	}

	results = (*env)->NewObject(env, classes.fetchResults, constructor, (jlong) n_start + (jlong) n_step, (jlong) n_end, (jlong) n_step, results_legends, result_columns);

	theend:
	// Free up RRD allocations
	for (i=0;i<col_cnt;i++)
		free(n_legend_v[i]);
	free(n_legend_v);
	free (n_data);

	return results;
}
