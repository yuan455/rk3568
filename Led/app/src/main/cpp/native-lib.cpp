#include <jni.h>
#include <string>
#include <android/log.h>
#include "include/gpiod.h"

enum
{
    LED_R=0,
    LED_Y,
    LED_G,
    LED_MAX,
};

enum
{
    ON=0,
    OFF,
};

typedef struct led_gpio_s
{
    int                           idx;
    int                           gpio;
    const char                   *desc;
    struct gpiod_line            *line;
}led_gpio_t;

led_gpio_t leds[LED_MAX] =
        {
                {LED_R, 18, "red", NULL},
                {LED_Y, 22, "yellow", NULL},
                {LED_G, 20, "green", NULL},
        };

extern "C"
JNIEXPORT void JNICALL
Java_com_example_myapplication_HardCtrl_ledOpen(JNIEnv *env, jclass clazz) {
    // TODO: implement ledOpen()
    struct gpiod_line  *line;
    struct gpiod_chip  *chip;
    const char         *chipname = "gpiochip0";
    int                 ret;
    int                 i, j;
    int                 rv = 0;

    chip = gpiod_chip_open_by_name(chipname);
    if( !chip )
    {
        __android_log_print(ANDROID_LOG_INFO, "LEDDemo", "gpiod open '%s' failed:%s\n", chipname, strerror(errno));
        return;
    }

    for( i=0; i<LED_MAX; i++ )
    {
        // 获取句柄
        leds[i].line = gpiod_chip_get_line(chip, leds[i].gpio);
        if( !leds[i].line )
        {
            __android_log_print(ANDROID_LOG_INFO, "LEDDemo", "Get line[%d] failed:%s\n", leds[i].gpio, strerror(errno));
            gpiod_line_release(leds[i].line);
            gpiod_chip_close(chip);
            break;
        }

        // 判断该GPIO接口是否被占用
        ret = gpiod_line_is_used(leds[i].line);
        if( ret )
        {
            __android_log_print(ANDROID_LOG_INFO, "LEDDemo", "line[%d] is used!\n", leds[i].gpio);
            gpiod_chip_close(chip);
            break;
        }

        // 设置该接口为输出模式
        ret = gpiod_line_request_output(leds[i].line, leds[i].desc, 0);
        if( ret < 0 )
        {
            __android_log_print(ANDROID_LOG_INFO, "LEDDemo", "set line[%d] output failed:%s\n", leds[i].gpio, strerror(errno));
            gpiod_line_release(leds[i].line);
            gpiod_chip_close(chip);
            break;
        }
    }
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_myapplication_HardCtrl_ledCtrl(JNIEnv *env, jclass clazz, jint which,
                                                jint status) {
    // TODO: implement ledCtrl()
    int rv;
    if( which<0 || which>=LED_MAX )
    {
        return -1;
    }
    __android_log_print(ANDROID_LOG_DEBUG, "LEDDemo", "turn %s led GPIO#%d %s\n", leds[which].desc, leds[which].gpio, status?"off":"on");

    if( ON == status )
    {
        rv = gpiod_line_set_value(leds[which].line, 0);
        if( rv < 0 )
        {
            __android_log_print(ANDROID_LOG_INFO, "LEDDemo", "turn %s ON failed:%s\n", leds[which].desc, strerror(errno));
            return -1;
        }
    }
    else
    {
        rv = gpiod_line_set_value(leds[which].line, 1);
        if( rv < 0 )
        {
            __android_log_print(ANDROID_LOG_INFO, "LEDDemo", "turn %s OFF failed:%s\n", leds[which].desc, strerror(errno));
            return -1;
        }
    }
    return 0;
}