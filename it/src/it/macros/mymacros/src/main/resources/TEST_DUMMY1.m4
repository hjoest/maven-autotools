# First dummy macro
AC_DEFUN([TEST_DUMMY1],
[
    AC_MSG_CHECKING([for a working dummy macro])
    DUMMY1_ENABLED=42
    AC_SUBST(DUMMY1_ENABLED)
])
