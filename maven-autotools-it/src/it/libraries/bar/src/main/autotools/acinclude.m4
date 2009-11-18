# Find the Java Development Kit
AC_DEFUN([AC_JAVA_JDK],
[

    AC_MSG_CHECKING([for Java Development Kit])

    AC_ARG_WITH([jdk],AC_HELP_STRING([--with-jdk=DIR],[JDK home directory]),[
        JDK_HOME=${withval}
        if test ! -d "${JDK_HOME}"
        then
            AC_MSG_ERROR([Not a directory: ${JDK_HOME}])
        fi
    ],[
        if test x"${JDK_HOME}" = x && test -n "${JAVA_HOME}"
        then
            JDK_HOME=${JAVA_HOME}
        fi
        if test x"${JDK_HOME}" != x && test ! -d "${JDK_HOME}"
        then
            AC_MSG_WARN([Not a directory: ${JDK_HOME}])
            JDK_HOME=""
        fi
        if test x"${JDK_HOME}" != x && test ! -e "${JDK_HOME}/bin/javac"
        then
            JDK_HOME=""
        fi
        if test "${JDK_HOME}" != "${JAVA_HOME}"
        then
            AC_MSG_ERROR([Environment variables JDK_HOME and JAVA_HOME pointing to different locations])
        fi
    ])

    if test x"${JDK_HOME}" = x
    then
        ac_jdk_favourite=$(
            for ac_jdk_places in \
                /usr/local /usr/local/java /usr/local/lib \
                /usr /usr/java /usr/lib \
                /opt  \
                C:/ C:/Program\ Files C:/Programme \
                /
            do
                for ac_jdk_prefix in jdk j2sdk java
                do
                    for ac_jdk_candidate in ${ac_jdk_places}/${ac_jdk_prefix}*
                    do
                        if test -e "${ac_jdk_candidate}/bin/javac" \
                            && test -e "${ac_jdk_candidate}/bin/java"
                        then
                            echo "`${ac_jdk_candidate}/bin/java -version 2>&1 | grep '^java version '` $ac_jdk_candidate"
                        fi
                    done
                done
            done | sort | tail -n 1)
        JDK_HOME="`echo ${ac_jdk_favourite} | sed 's,^java version ".*" ,,'`"
    fi

    if test x"${JDK_HOME}" = x
    then
        AC_MSG_ERROR([Could not find a valid JDK, please specify one with --with-jdk option (run ./configure --help for more options)])
    fi

    JAVA_VERSION="`${JDK_HOME}/bin/java -version 2>&1 | grep '^java version ' | sed 's,^java version "\(.*\)",\1,'`"

    AC_MSG_RESULT([${JDK_HOME} (java version ${JAVA_VERSION})])
    AC_SUBST(JAVA_VERSION)
    AC_SUBST(JDK_HOME)

    ac_tmp_cppflags="$CPPFLAGS"
    ac_jdk_os=`echo $build_os | sed 's,[-0-9].*,,' | sed 's,cygwin,win32,'`
    if test $ac_jdk_os = win32; then
        ac_jdk_include=`cygpath -m "$JDK_HOME"`/include
    else
        ac_jdk_include=$JDK_HOME/include
    fi
    CPPFLAGS="$ac_tmp_cppflags -I$ac_jdk_include -I$ac_jdk_include/$ac_jdk_os"
    AC_TRY_CPP([#include <jni.h>],[
        ac_tmp_cppflags="$CPPFLAGS"
    ],[
        AC_MSG_WARN([unable to include <jni.h>])
    ])
    CPPFLAGS="$ac_tmp_cppflags"

    AC_PROVIDE([$0])
])


# Check for WIN32/CYGWIN specific flags
AC_DEFUN([AC_WIN32_CYGWIN_FLAGS],
[

    AC_MSG_CHECKING([whether to enable WIN32/CYGWIN specific flags])

    ac_tmp_cppflags="$CPPFLAGS"
    ac_tmp_ldflags="$LDFLAGS"

    case "$host_os" in
    *cygwin*)
        win32=true
        AC_MSG_RESULT([yes])
        AC_CHECK_TOOL(WINDRES, windres)
        AC_CHECK_TOOL(DLLWRAP, dllwrap)
        nocygwin=""
        if test "X$1" = "Xno-cygwin" -o "X$1" = "Xnocygwin"; then
            nocygwin="-mno-cygwin"
        fi
        CPPFLAGS="$ac_tmp_cppflags $nocygwin -D_WIN32 -DUNICODE"
        LDFLAGS="$ac_tmp_ldflags $nocygwin"
        ;;
    *)
        win32=false
        AC_MSG_RESULT([no])
        ;;
    esac

    AM_CONDITIONAL(WIN32, test x$win32 = xtrue)
])


# Check for memwatch
AC_DEFUN([AC_CHECK_MEMWATCH],
[

    AC_MSG_CHECKING([whether memwatch is available])

    AC_CHECK_HEADER([memwatch.h],MW_CFLAGS="-DMEMWATCH")
    AC_SUBST(MW_CFLAGS)
    AC_CHECK_LIB(memwatch,mwMalloc,MW_LDFLAGS="-lmemwatch")
    AC_SUBST(MW_LDFLAGS)
])


# Check for code coverage tool
AC_DEFUN([AC_CHECK_COVERAGE],
[

    AC_MSG_CHECKING([for code coverage analysis tool])

    AC_CHECK_TOOL(GCOV, gcov)
    if test -n "${GCOV}"; then
        GCOV_CFLAGS="-fprofile-arcs -ftest-coverage"
        AC_CHECK_LIB(gcov,main,GCOV_LDFLAGS="-lgcov")
        cat >./coverage <<\_ACEOF
#!/bin/sh
set -e
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

