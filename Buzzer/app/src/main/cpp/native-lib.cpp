#include <jni.h>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <android/log.h>

#define TAG "BuzzerApp"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static char pwm_path[100];

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_buzzer_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_buzzer_MainActivity_pwmConfig(JNIEnv *env, jclass clazz, jstring attr,
                                               jstring val) {
    // TODO: implement pwmConfig()
    char file_path[100];
    char *attr_native;
    char *val_native;
    int  len;
    int  fd = -1;

    if(attr == NULL || val == NULL)
    {
        LOGE("argument invalid\n");
        return -1;
    }

    attr_native = (char *)env->GetStringUTFChars(attr, 0);
    val_native = (char *)env->GetStringUTFChars(val, 0);

    memset(file_path, 0, sizeof(file_path));
    snprintf(file_path, sizeof (file_path), "%s/%s", pwm_path, attr_native);
    fd = open(file_path, O_WRONLY);
    if (fd < 0)
    {
        LOGE("[%s] open %s error:%s\n", __FUNCTION__ , file_path, strerror(errno));
        return fd;
    }

    len = strlen(val_native);
    if(len != write(fd, val_native, len))
    {
        LOGE("[%s] write %s to %s error: %s\n", __FUNCTION__ , val_native, file_path, strerror(errno));
        close(fd);
        return -2;
    }

    close(fd);
    return 0;

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_buzzer_MainActivity_pwmOpen(JNIEnv *env, jclass clazz, jstring id) {
    // TODO: implement pwmOpen()
    char *id_native;
    char  temp[100];
    int   fd;
    id_native = (char *)env->GetStringUTFChars(id, 0);
    memset(pwm_path, 0, sizeof (pwm_path));
    snprintf(pwm_path, sizeof (pwm_path), "/sys/class/pwm/pwmchip%s/pwm0", id_native);

    memset(temp, 0, sizeof (temp));
    if(access(pwm_path, F_OK))
    {
        snprintf(temp, sizeof (temp), "/sys/class/pwm/pwmchip%s/export", id_native);
        fd = open(temp, O_WRONLY);
        if(fd < 0)
        {
            LOGE("open %s error:%s\n", temp, strerror(errno));
            return -1;
        }

        if(1 != write(fd, "0", 1))
        {
            LOGE("Write '0' to pwmchip%s/export error\n", id_native);
            close(fd);
            return -2;
        }
        close(fd);
    }
    return 0;
}
