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

#ifdef HAVE_STRING_H
#include <string.h>
#endif

#include "static-foo.h"

#define MIN(a,b) ((a)>(b)?(b):(a))

static const char *hello = "Hello";

int
static_foo(char *s, int n)
{
    int p;
    int w = MIN(n < 0 ? strlen(s) : n, strlen(hello));
    for (p = 0; p < w && s[p]; ++p) {
        s[p] = hello[p];
    }
    return p;
}
