# Process this file with autoconf to produce a configure script.
AC_INIT([bar],[1.0])
AC_CANONICAL_TARGET([])
AM_INIT_AUTOMAKE

# Minimum Autoconf version required.
AC_PREREQ(2.59)

# Where to generate output; srcdir location.
AC_CONFIG_HEADERS([config.h])
AC_CONFIG_SRCDIR([bar.c])

# Check for WIN32/CYGWIN specific flags
AC_WIN32_CYGWIN_FLAGS([cygwin])

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
AC_CHECK_HEADERS([stdio.h])

AC_CONFIG_FILES([Makefile])
AC_OUTPUT
