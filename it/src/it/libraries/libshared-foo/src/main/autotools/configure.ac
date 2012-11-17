# Process this file with autoconf to produce a configure script.
AC_INIT([shared-foo],[1.0])
AC_CANONICAL_TARGET([])
LT_INIT([shared,win32-dll,disable-static])
AM_INIT_AUTOMAKE

# Minimum Autoconf version required.
AC_PREREQ(2.61)

# Where to generate output; srcdir location.
AC_CONFIG_SRCDIR([shared-foo.c])
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_MACRO_DIR([m4])

# Check for Cygwin specifics
MVNAT_CYGWIN_FLAGS([nocygwin])
MVNAT_MACOSX_BUNDLE

# Don't build shared libs.
AC_DISABLE_SHARED

# Checks for libtool.
AC_PROG_LIBTOOL

# Checks for programs.
AC_PROG_CC
AC_ISC_POSIX

AM_PROG_CC_C_O

# Checks for header files.
AC_HEADER_STDC([])
AC_CHECK_HEADERS([string.h])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
