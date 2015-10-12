%ifnarch x86_64
%{!?_libdir: %define _libdir %_prefix/lib}
%endif

%ifarch x86_64
%{!?_libdir: %define _libdir %_prefix/lib64}
%endif

Name: jrrd2
Version: 2.0.2
Release: 1
License: GPL
Group: Applications/Databases
Summary: Java interface to RRDTool
Source: %{name}-%{version}.tar.gz
Source1: apache-maven-3.2.5-bin.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-root

BuildRequires: cmake >= 2.6.4, rrdtool-devel >= 1.5.0, java7-sdk, gcc

%description
A Java interface to the RRDTool round-robin database.

%package devel
Summary: Development files for JRRD2
Group: Applications/Databases
Requires: %{name} = %{version}

%description devel
Javadoc and Java source for JRRD2

%prep
%setup -n %{name}-%{version}
%setup -a 1

%build
MYDIR=`pwd`
if [ -d /usr/lib/jvm/java-1.7.0 ]; then
	export JAVA_HOME="/usr/lib/jvm/java-1.7.0"
	export PATH="$JAVA_HOME/bin:$PATH"
fi
export PATH="$MYDIR/apache-maven-3.2.5/bin:$PATH"
./build.sh || exit 1

%install
install -d -m 755 ${RPM_BUILD_ROOT}%{_libdir}
install -c -m 755 dist/libjrrd2.so ${RPM_BUILD_ROOT}%{_libdir}/

install -d -m 755 ${RPM_BUILD_ROOT}%{_datadir}/java
install -c -m 644 dist/jrrd2-api-*.jar ${RPM_BUILD_ROOT}%{_datadir}/java/jrrd2.jar

find $RPM_BUILD_ROOT%{_datadir}/java ! -type d | \
	sed -e "s|^${RPM_BUILD_ROOT}|%attr(644,root,root) |" | \
	grep -v -E '(javadoc|sources)' | \
	sort > %{_tmppath}/files.jrrd2

find $RPM_BUILD_ROOT%{_datadir}/java ! -type d | \
	sed -e "s|^${RPM_BUILD_ROOT}|%attr(644,root,root) |" | \
	grep -E '(javadoc|sources)' | \
	sort > %{_tmppath}/files.jrrd2-devel

%clean
if [ "$RPM_BUILD_ROOT" != "/" ]; then
	rm -rf "$RPM_BUILD_ROOT"
fi

%files -f %{_tmppath}/files.jrrd2
%attr(755,root,root) %{_libdir}/libjrrd2*

%files devel -f %{_tmppath}/files.jrrd2-devel
