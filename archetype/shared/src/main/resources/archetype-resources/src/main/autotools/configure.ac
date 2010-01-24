# Process this file with autoconf to produce a configure script.
AC_PREREQ(2.61)
AC_INIT([${artifactId}],[${version}])
AM_INIT_AUTOMAKE
AC_CONFIG_SRCDIR([hello.c])
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_MACRO_DIR([m4])

# Checks for programs.
AC_PROG_CC
AM_PROG_CC_C_O

# Don't build static libs.
AC_DISABLE_STATIC

# Tell libtool to build a DLL on Windows
AC_LIBTOOL_WIN32_DLL

AC_PROG_LIBTOOL

# Check for Cygwin specifics
MVNAT_CYGWIN_FLAGS([no-cygwin])

# Find the Java SDK
MVNAT_JAVA_JDK

# Checks for libraries.

# Checks for header files.
AC_HEADER_STDC([])
AC_CHECK_HEADERS([windows.h])

# Checks for typedefs, structures, and compiler characteristics.

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
