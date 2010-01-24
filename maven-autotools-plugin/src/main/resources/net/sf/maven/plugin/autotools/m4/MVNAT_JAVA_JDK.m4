# Find the Java development kit
AC_DEFUN([MVNAT_JAVA_JDK],
[

    AC_MSG_CHECKING([for Java SDK])

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
    ac_jdk_os=`echo $build_os | sed 's,[-0-9].*,,' | sed 's,x-gnu,x,' | sed 's,cygwin,win32,'`
    if test x$ac_jdk_os = xwin32; then
        ac_jdk_include=`cygpath -u "$JDK_HOME"`/include
    else
        ac_jdk_include=$JDK_HOME/include
    fi
    CPPFLAGS="$ac_tmp_cppflags -I$ac_jdk_include -I$ac_jdk_include/$ac_jdk_os"
    AC_TRY_COMPILE([#include <jni.h>],[jlong conf_dummy;],[
        ac_tmp_cppflags="$CPPFLAGS"
    ],[
        CPPFLAGS="$CPPFLAGS -D__int64=int64_t"
        AC_TRY_COMPILE([#include <jni.h>],[jlong conf_dummy;],[
            ac_tmp_cppflags="$CPPFLAGS"
        ],[
            AC_MSG_WARN([unable to include <jni.h>])
        ])
    ])
    CPPFLAGS="$ac_tmp_cppflags"

    AC_PROVIDE([$0])
])
