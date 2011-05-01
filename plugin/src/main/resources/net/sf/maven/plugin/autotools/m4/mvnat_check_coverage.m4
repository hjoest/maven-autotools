# Check for code coverage tool
AC_DEFUN([MVNAT_CHECK_COVERAGE],
[

    AC_MSG_CHECKING([for code coverage analysis tool])

    AC_CHECK_TOOL(GCOV, gcov)
    if test -n "${GCOV}"; then
        GCOV_CFLAGS="-fprofile-arcs -ftest-coverage"
        AC_CHECK_LIB(gcov,main,GCOV_LDFLAGS="-lgcov")
        cat >./coverage <<\_ACEOF
#!/bin/sh
set -e
LANG=C
_ACEOF
        echo -n "if test \$" >>./coverage
        echo "# -lt 3; then" >>./coverage
        cat >>./coverage <<\_ACEOF
    echo "Usage: coverage <minpercent> <executable> <gcda>" >&2
    exit 1
fi
_ACEOF
        echo -n "mincov=\"\$" >>./coverage
        echo "1\"" >>./coverage
        echo -n "executable=\"\$" >>./coverage
        echo "2\"" >>./coverage
        echo -n "gcda=\"\$" >>./coverage
        echo "3\"" >>./coverage
        cat >>./coverage <<\_ACEOF
IFS=""
covres=`gcov -o $gcda $executable | grep -B 1 "Lines executed:" | grep -A 1 "\.c'$"`
percent=$(echo $covres | grep "^Lines executed:" | sed -e "s/^Lines executed://" -e "s/\.[[0-9]]*% of [[0-9]]*//")
file=$(echo $covres | grep "^File " | sed -e "s/^File '//" -e "s/'$//")
if test "$percent" -lt "$mincov"; then
    echo "Code coverage is $percent% (less than $mincov%) in file $file" >&2
    exit 1
fi
echo "$covres"
exit 0
_ACEOF

    else
        echo "#!/bin/sh" > ./coverage
    fi

    chmod +x ./coverage

    AC_SUBST(GCOV_CFLAGS)
    AC_SUBST(GCOV_LDFLAGS)
])
