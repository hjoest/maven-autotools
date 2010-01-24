lib_LTLIBRARIES = libhello.la
libhello_la_SOURCES = hello.c
libhello_la_CFLAGS = $(AM_CFLAGS)
libhello_la_LDFLAGS = -release "${version}" -no-undefined

AM_CFLAGS = -Wall -g
AM_LDFLAGS = -g
INCLUDES = -I @srcdir@
ACLOCAL_AMFLAGS = -I m4
