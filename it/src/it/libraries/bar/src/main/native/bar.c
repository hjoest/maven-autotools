/*
 * Copyright (C) 2006-2013 Holger Joest <holger@joest.org>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#ifdef HAVE_STDIO_H
#include <stdio.h>
#endif
#ifdef HAVE_LIMITS_H
#include <limits.h>
#endif
#ifdef HAVE_STRING_H
#include <string.h>
#endif
#ifdef HAVE_DLFCN_H
#include <dlfcn.h>
#endif
#ifdef HAVE_WINDOWS_H
#include <windows.h>
#endif

#include <static-foo.h>
#include <shared-foo.h>

#define MAX(a,b) ((a)>(b)?(a):(b))
#define ARRAY_SIZE(a) (sizeof(a) / sizeof(a[0]))


void
rchop_path(char *path, char *chopped)
{
    int n = strlen(path);
    while ((--n) > 0 && path[n] != '/' && path[n] != '\\');
    if (chopped != NULL) {
        strncpy(chopped, &path[n], FILENAME_MAX);
    }
    path[n] = '\0';
}


void
rappend_path(char *path, char *append)
{
    int n = strlen(path);
    strncat(path, append, MAX(FILENAME_MAX, PATH_MAX - n));
}


void
make_libshared_path(char *path, char *argv0)
{
    char chopped_os[FILENAME_MAX + 1];
    char chopped_arch[FILENAME_MAX + 1];
    char *rchop[] = { NULL, chopped_os, chopped_arch, NULL, NULL };
    char *rappend[] = { "/dependencies", "/lib", chopped_arch, chopped_os };
    int p;

    strncpy(path, argv0, PATH_MAX);

    for (p = 0; p < ARRAY_SIZE(rchop); ++p) {
        rchop_path(path, rchop[p]);
    }
    for (p = 0; p < ARRAY_SIZE(rappend); ++p) {
        rappend_path(path, rappend[p]);
    }

    rappend_path(path,
#if defined(__APPLE__) || defined(MACOSX)
                 "/libshared-foo-1.0.bundle"
#elif defined(WINDOWS) || defined(WIN32)
                 "/libshared-foo-1-0.dll"
#else
                 "/libshared-foo-1.0.so"
#endif
                 );
}


/* Just a poor man's wrapper of dlopen on top of the Windows API. */
#if (defined(WINDOWS) || defined(WIN32)) && !defined(HAVE_DLFCN_H)

#define RTLD_LAZY 1

void *
dlopen(char *path, int mode)
{
    HMODULE handle = LoadLibrary(path);
    return (void *) handle;
}

void *
dlsym(void* handle, char* symbol)
{
    FARPROC address = GetProcAddress(handle, symbol);
    return (void *) address;
}

char *
dlerror(void)
{
    return "";
}

#endif


int
main(int argc, char **argv)
{
    char libshared[PATH_MAX + 1];
    char result[] = "This failed";
    void *dlp;
    shared_foo_t shared_foo;

    int n = static_foo(result, -1);
    result[n] = ' ';

    make_libshared_path(libshared, argv[0]);
    dlp = dlopen(libshared, RTLD_LAZY);
    if (dlp == NULL) {
        fprintf(stderr, "dlopen: %s\n", dlerror());
        return -1;
    }
    shared_foo = (shared_foo_t) dlsym(dlp, "shared_foo");
    if (shared_foo == NULL) {
        fprintf(stderr, "dlsym: %s\n", dlerror());
        return -1;
    }

    shared_foo(&result[n + 1], -1);

    printf("%s\n", result);
    return 0;
}
