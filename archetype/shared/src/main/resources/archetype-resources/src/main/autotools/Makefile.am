lib_LTLIBRARIES = lib${artifactId}.la
lib${artifactId}_la_SOURCES = hello.c
lib${artifactId}_la_CFLAGS = $(AM_CFLAGS)
lib${artifactId}_la_LDFLAGS = -release "${version}" -no-undefined
AM_CPPFLAGS = -I @srcdir@
AM_CFLAGS = -Wall -g
AM_LDFLAGS = -g
ACLOCAL_AMFLAGS = -I m4
