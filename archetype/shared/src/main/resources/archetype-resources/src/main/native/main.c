#ifdef HAVE_CONFIG_H
\#include "config.h"
#endif
#ifdef HAVE_STDIO_H
\#include <stdio.h>
#endif
\#include "hello.h"


int
main(int argc, char **argv)
{
    printf("%s\n", greeting());
    return 0;
}

