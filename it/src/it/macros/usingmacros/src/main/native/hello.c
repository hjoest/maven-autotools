#include <stdio.h>

int
main(int argc, char **argv)
{
#if (TEST_VALUE == 42)
    printf("Hello World\n");
#endif
    return 0;
}

