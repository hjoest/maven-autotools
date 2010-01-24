# Process this file with autoconf to produce a configure script.
AC_PREREQ(2.61)
AC_INIT([hello],[1.0])
AM_INIT_AUTOMAKE
AC_CONFIG_SRCDIR([hello.c])
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_MACRO_DIR([m4])

# Checks for programs.
AC_PROG_CC
AM_PROG_CC_C_O
AC_PROG_LIBTOOL

# Check for Cygwin specifics
MVNAT_CYGWIN_FLAGS([cygwin])

# Checks for libraries.

# Checks for header files.
AC_HEADER_STDC([])
AC_CHECK_HEADERS([stdio.h])

# Checks for typedefs, structures, and compiler characteristics.

# Checks for library functions.

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
