# The release number is set to 0 unless overridden
%{!?releasenumber:%define releasenumber 0}

%define _libdir %_prefix/lib

%ifarch x86_64
%define _libdir %_prefix/lib64
%endif

Name:           jrrd2
Version:        %version
Release:        %releasenumber
Summary:        JNI interface to rrdtool 

License:        AGPL
URL:            https://github.com/OpenNMS/jrrd2 
Source:         %{name}-source-%{version}-%{releasenumber}.tar.gz

BuildRequires: rrdtool-devel, jdk >= 1.7.0, gcc, cmake 
Requires:      rrdtool-devel 

%description
A thread-safe JNI interface to rrdtool.


%prep
%setup -n %{name}-%{version}-%{releasenumber}


%build
./build.sh


%install
install -D -m 755 $RPM_BUILD_DIR/%{name}-%{version}-%{releasenumber}/dist/libjrrd2.so $RPM_BUILD_ROOT%{_libdir}/libjrrd2.so
install -D -m 755 $RPM_BUILD_DIR/%{name}-%{version}-%{releasenumber}/dist/jrrd2.jar $RPM_BUILD_ROOT%{_datadir}/java/jrrd2.jar


%files
%attr(755,root,root) %{_libdir}/libjrrd*
%attr(644,root,root) %{_datadir}/java/*.jar


%doc



%changelog
* Fri Mar 27 2015 Jesse White
- Initial release 
