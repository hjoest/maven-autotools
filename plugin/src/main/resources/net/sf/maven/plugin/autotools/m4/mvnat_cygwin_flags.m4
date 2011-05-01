# Check for CYGWIN specific flags
AC_DEFUN([MVNAT_CYGWIN_FLAGS],
[

    AC_MSG_CHECKING([whether to enable CYGWIN specific flags])

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
