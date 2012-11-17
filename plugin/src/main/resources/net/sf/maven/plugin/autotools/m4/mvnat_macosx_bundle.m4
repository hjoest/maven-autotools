# Build a MacOSX bundle
AC_DEFUN([MVNAT_MACOSX_BUNDLE],
[

    ac_tmp_ldflags="$LDFLAGS"

    case "$host_os" in
    *darwin*|*rhapsody*|*macosx*)
        if grep -q " -module" <<<$ac_tmp_ldflags; then
            LDFLAGS="$ac_tmp_ldflags -shrext .bundle"
        else
            LDFLAGS="$ac_tmp_ldflags -module -shrext .bundle"
        fi
        ;;
    esac

])
