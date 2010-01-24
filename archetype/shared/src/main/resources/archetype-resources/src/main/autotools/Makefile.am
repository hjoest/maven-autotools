lib_LTLIBRARIES = lib${artifactId}.la
lib${artifactId}_la_SOURCES = hello.c
lib${artifactId}_la_CFLAGS = $(AM_CFLAGS)
lib${artifactId}_la_LDFLAGS = -release "${version}" -no-undefined

AM_CFLAGS = -Wall -g
AM_LDFLAGS = -g
INCLUDES = -I @srcdir@
ACLOCAL_AMFLAGS = -I m4
