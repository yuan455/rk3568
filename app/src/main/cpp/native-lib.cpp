#include <jni.h>
#include <string>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <android/log.h>

#define TAG "SerialApp"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

int fd = -1; // 表示操作的串口
int mSend_len; // 单次发送的最大长度

// 将int类型的波特率转换成可以识别的波特率
static speed_t getBaudRate(int baudRate){
    switch (baudRate) {
        case 0:
            return B0;
        case 50:
            return B50;
        case 75:
            return B75;
        case 110:
            return B110;
        case 134:
            return B134;
        case 150:
            return B150;
        case 200:
            return B200;
        case 300:
            return B300;
        case 600:
            return B600;
        case 1200:
            return B1200;
        case 1800:
            return B1800;
        case 2400:
            return B2400;
        case 4800:
            return B4800;
        case 9600:
            return B9600;
        case 19200:
            return B19200;
        case 38400:
            return B38400;
        case 57600:
            return B57600;
        case 115200:
            return B115200;
        case 230400:
            return B230400;
        case 460800:
            return B460800;
        case 500000:
            return B500000;
        case 576000:
            return B576000;
        case 921600:
            return B921600;
        case 1000000:
            return B1000000;
        case 1152000:
            return B1152000;
        case 1500000:
            return B1500000;
        case 2000000:
            return B2000000;
        case 2500000:
            return B2500000;
        case 3000000:
            return B3000000;
        case 3500000:
            return B3500000;
        case 4000000:
            return B4000000;
        default:
            return -1;
    }
}

extern "C"
JNIEXPORT int JNICALL
Java_com_example_serial_SerialControl_openSerialPort(JNIEnv *env, jclass clazz, jstring path,
                                                     jint baud_rate, jint data_bits, jint parity,
                                                     jint stop_bits, jint flow_control, jint max_len) {
    // TODO: implement openSerialPort()
    speed_t speed;
    mSend_len = max_len;

    // 检查波特率是否合法
    {
        speed = getBaudRate(baud_rate);
        if (speed == -1)
        {
            LOGE("Invalid buad rate");
            return -1;
        }
    }

    // 打开串口设备
    {
        jboolean iscopy;
        // 获取String字符串path中的UTF8编码，并将其转换成C中的字符串
        const char* path_utf = (*env).GetStringUTFChars(path, &iscopy);
        LOGD("Opening serial port %s with flags 0x%x", path_utf, O_RDWR | O_NOCTTY | O_NONBLOCK);
        fd = open(path_utf, O_RDWR | O_NOCTTY | O_NONBLOCK);
        if (fd < 0)
        {
            LOGE("open port failed:%s\n", strerror(errno));
            return -1;
        }
        LOGD("open() fd=%d", fd);
        // 释放JNI字符串的UTF8编码
        (*env).ReleaseStringUTFChars(path, path_utf);

        // 检查串口是否处于阻塞态
        if(fcntl(fd, F_SETFL, 0) < 0)
        {
            LOGE("Fcntl check failed:%s\n", strerror(errno));
            return -1;
        }
        // 检查该文件描述符是否对应了终端设备
        if( 0 == isatty(fd) )
        {
            LOGE("%s: [%d] is not a Terminal equipment.\n", path_utf, fd);
            return -1;
        }

        LOGD("Open %s successfully.\n", path_utf);
    }

    {
        struct termios cfg;
        LOGD("starting configuring serial port");
        // 获取fd的串口属性并保存在cfg中。调用成功返回0，否则返回非零值
        if(tcgetattr(fd, &cfg))
        {
            LOGD("tecgetattr() failed:%s", strerror(errno));
            close(fd);
            return -1;
        }

        // 将串口的配置设置为原始模式：串口不会对数据进行任何处理，而是直接传输，通常用于需要直接访问串口设备的情况
        cfmakeraw(&cfg);
        cfsetispeed(&cfg, speed);
        cfsetospeed(&cfg, speed);

        // ~CSIZE是对 CSIZE 取反，它的作用是将数据位设置的部分清零，即取消所有数据位的设置。
        cfg.c_cflag &= ~CSIZE;
        switch (data_bits) {
            case 5:
                cfg.c_cflag |= CS5; // 使用5位数据位, |=表示按位或赋值
                break;
            case 6:
                cfg.c_cflag |= CS6;
                break;
            case 7:
                cfg.c_cflag |= CS7;
                break;
            case 8:
                cfg.c_cflag |= CS8;
                break;
            default:
                cfg.c_cflag |= CS8;
                break;
        }

        switch (parity) {
            case 0:
                cfg.c_cflag &= ~PARENB; // 将控制模式标志中的奇偶校验使能位清零，表示禁用奇偶校验。
                break;
            case 1:
                cfg.c_cflag |= (PARODD |PARENB); //奇校验
            case 2:
                cfg.c_iflag &= ~(IGNPAR | PARMRK); // 清除输入模式标志中的忽略奇偶校验错误和标记奇偶校验错误的位。
                cfg.c_iflag |= INPCK; // 设置输入模式标志中的奇偶校验使能位，表示开启奇偶校验。
                cfg.c_cflag |= PARENB; // 设置控制模式标志中的奇偶校验使能位，表示开启奇偶校验。
                cfg.c_cflag &= ~PARODD; // 将控制模式标志中的奇偶校验位设置为偶校验。
                break;
            default:
                cfg.c_cflag &= ~PARENB;
                break;
        }

        switch (stop_bits) {
            case 1:
                cfg.c_cflag &= ~CSTOPB;
                break;
            case 2:
                cfg.c_cflag |= CSTOPB;
                break;
            default:
                cfg.c_cflag &= ~CSTOPB; //默认使用1位作为停止位
                break;
        }

        switch (flow_control) {
            case 1: // Hard control: RTS/CTS
                cfg.c_cflag |= CRTSCTS;
                cfg.c_iflag &= ~(IXON | IXOFF);
                break;
            case 2: // Software control
                cfg.c_cflag &= ~(CRTSCTS);
                cfg.c_iflag |= (IXON | IXOFF);
                break;
            case 0: // None
            default:
                cfg.c_cflag &= ~(CRTSCTS);
                cfg.c_iflag &= ~(IXON | IXOFF);
                break;
        }

        if (tcflush(fd, TCIFLUSH))
        {
            LOGE("Falied to clear the cache:%s\n", strerror(errno));
            return -1;
        }

        // 将设置的串口参数应用到串口上，TCSANOW表示立即生效，（TCSADRAIN：在所有输出都被传输后生效；TCSAFLUSH：在所有输出都被传输后生效，同时丢弃所有未读取的输入）
        if(tcsetattr(fd, TCSANOW, &cfg))
        {
            LOGE("tcsetattr() failed:%s", strerror(errno));
            close(fd);
            return -1;
        }
        LOGD("Open and configure serial port successfully\n");
    }
    return 0;
}
extern "C"
JNIEXPORT int JNICALL
Java_com_example_serial_SerialControl_closeSerialPort(JNIEnv *env, jclass clazz) {
    // TODO: implement closeSerialPort()
    struct termios cfg;

    if (fd == -1)
    {
        LOGE("fd = -1\n");
    }

    // 清空用于串口通信的缓冲区
    if (tcflush(fd, TCIOFLUSH))
    {
        LOGE("Tcflush failed:%s\n", strerror(errno));
        return -1;
    }

    // 将串口设置为原有属性
    if(tcsetattr(fd, TCSANOW, &cfg))
    {
        LOGE("Set old options failed:%s\n", strerror(errno));
        return -2;
    }

    close(fd);
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serial_SerialControl_sendToPort(JNIEnv *env, jclass clazz, jstring msg, jint len) {
    // TODO: implement sendToPort()
    // msg: 表示发送的数据；len: 发送的长度
    int   rv;
    char *ptr, *end;
    char *sendMsg;
    sendMsg = reinterpret_cast<char *>(msg);

    if( (fd == -1) || !msg || len <= 0 )
    {
        LOGE("Invalid parameter.\n");
        return -1;
    }

    if (len > mSend_len)
    {
        ptr = sendMsg;
        end = sendMsg + len;

        do {
            if( mSend_len < (end-ptr) )
            {
                rv = write(fd, ptr, mSend_len);
                if (rv <= 0 || rv != mSend_len)
                {
                    LOGE("Write to port[%d] failed:%s\n", fd, strerror(errno));
                    return -1;
                }

                ptr += mSend_len;
            }
            else
            {
                rv = write(fd, ptr, (end-ptr));
                if (rv <= 0 || rv != (end-ptr))
                {
                    LOGE("Write to port[%d] failed:%s\n", fd, strerror(errno));
                    return -1;
                }
                ptr += (end - ptr);
            }
        } while (end > ptr);
    }
    else
    {
        rv = write(fd, sendMsg, len);
        if( rv <= 0 || rv != len )
        {
            LOGE("Write to port[%d] failed:%s\n", fd, strerror(errno));
            return -1;
        }
        LOGD("Write to port[%d] successfully\n", fd);
    }
    return rv;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serial_SerialControl_recvFromPort(JNIEnv *env, jclass clazz, jstring msg, jint len,
                                                   jint timeout) {
    // TODO: implement recvFromPort()
    int             rv;
    fd_set          rset;
    struct timeval  time_out;

    if( fd == -1 || !msg || len <= 0 )
    {
        LOGE("Invalid parameter.\n");
        return -1;
    }

    if (timeout)
    {
        time_out.tv_sec = (time_t)timeout;
        time_out.tv_usec = 0;

        FD_ZERO(&rset);
        FD_SET(fd, &rset);

        rv = select(fd+1, &rset, NULL, NULL, &time_out);
        if( rv < 0 )
        {
            LOGE("Select failed:%s\n", strerror(errno));
            return -1;
        }
        else if( 0 == rv )
        {
            LOGD("Timeout....\n");
            return 0;
        }
    }

    usleep(1000);
    rv = read(fd, msg, len);
    if ( rv <= 0 )
    {
        LOGE("Read failed:%s\n", strerror(errno));
        return -1;
    }
    LOGD("Read data from port successfully\n");
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serial_SerialControl_saveToFile(JNIEnv *env, jclass clazz, jstring msg, jint len,
                                                 jstring file_name) {
    // TODO: implement saveToFile()
    int mFd = -1;
    int rv;
    mFd = open(reinterpret_cast<const char *const>(file_name), O_RDWR | O_APPEND);
    rv = write(fd, msg, len);
    if (rv < 0) {
        LOGE("Save data to file failed:%s\n" , strerror(errno));
        return -1;
    }
    close(mFd);
    return rv;
}