bin_PROGRAMS = hello
hello_SOURCES = main.c
hello_CFLAGS = $(AM_CFLAGS) $(MW_CFLAGS)
hello_LDADD = -lhello

lib_LTLIBRARIES = libhello.la
libhello_la_SOURCES = hello.c
libhello_la_CFLAGS = $(AM_CFLAGS)

AM_CFLAGS = -Wall -g
AM_LDFLAGS = -g
INCLUDES = -I @srcdir@
ACLOCAL_AMFLAGS = -I m4
