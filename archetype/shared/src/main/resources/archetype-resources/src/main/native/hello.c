#ifdef HAVE_CONFIG_H
\#include "config.h"
#endif
\#include "hello.h"
\#include "jni-${artifactId}.h"


const char *
greeting(void)
{
    return "Hello World!";
}


#set( $jnipackage = $package.replace('.','_') )
JNIEXPORT jstring JNICALL
Java_${jnipackage}_App_getGreeting0(JNIEnv *env, jobject thiz)
{
    return (*env)->NewStringUTF(env, greeting());
}

