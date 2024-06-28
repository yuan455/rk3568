#include <jni.h>
#include <string>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <android/log.h>
#include "include/gpiod.h"

#define TAG "SerialApp"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

typedef struct comport_s
{
    char            devname[12];
    unsigned int    databit, parity, stopbit, flowctrl;
    long            baudrate;

    int             fd;
    int             frag_size;
}comport_t;

comport_t *comport;

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
                                                     jlong baud_rate, jint data_bits, jint parity,
                                                     jint stop_bits, jint flow_control, jint max_len) {
    // TODO: implement openSerialPort()
    speed_t        speed;
    struct termios old_cfg, new_cfg;
    int            old_flags;
    long           temp;

    comport = (comport_t *) malloc(sizeof(comport_t));
    if (comport == NULL) {
        LOGE("Failed to allocate memory for comport structure.");
        return -1;
    }
    memset(comport, 0, sizeof(comport));

//    mSend_len = max_len;

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
        const char* devname = (*env).GetStringUTFChars(path, &iscopy);
        LOGD("Opening serial port %s with flags 0x%x", devname, O_RDWR);
        strncpy(comport->devname, devname, 12);
        comport->baudrate = baud_rate;
        comport->fd = -1;
        comport->frag_size = 128;
        comport->databit = data_bits;
        comport->parity = parity;
        comport->flowctrl = flow_control;
        comport->stopbit = stop_bits;

        if( !strstr(comport->devname, "tty") )
        {
            LOGE("Open Not a tty device \" %s\" \n", comport->devname);
            return -2;
        }

        comport->fd = open(comport->devname, O_RDWR);
        if ( comport->fd < 0 )
        {
            LOGE("open port failed:%s\n", strerror(errno));
            return -3;
        }
        LOGD("Open device %s", comport->devname);
        // 释放JNI字符串的UTF8编码
        (*env).ReleaseStringUTFChars(path, devname);

        if( (-1 != (old_flags = fcntl(comport->fd, F_GETFL, 0))) && (-1 != fcntl(comport->fd, F_SETFL, old_flags & ~O_NONBLOCK)))
        {
            if(-1 == tcflush(comport->fd, TCIOFLUSH))
            {
                LOGE("open port failed:%s\n", strerror(errno));
                return -4;
            }
        }
        else
        {
            LOGE("open port failed:%s\n", strerror(errno));
            return -5;
        }

        if ( 0 != tcgetattr(comport->fd, &old_cfg))
        {
            LOGE("open port failed:%s\n", strerror(errno));
            return -6;
        }
    }

    {
        LOGD("starting configuring serial port");

        old_cfg.c_cflag &= ~CSIZE;
        old_cfg.c_lflag &= ~ICANON;
        old_cfg.c_lflag &= ~ECHO;
        old_cfg.c_lflag &= ~ECHOE;
        old_cfg.c_lflag &= ~ECHONL;
        old_cfg.c_lflag &= ~ISIG;
        old_cfg.c_iflag &= ~(IXON | IXOFF | IXANY);
        old_cfg.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL);
        old_cfg.c_oflag &= ~OPOST;
        old_cfg.c_oflag &= ~ONLCR;
        old_cfg.c_cflag |= CREAD | CLOCAL;

        switch (comport->databit)
        {
            case 7:
                old_cfg.c_cflag |= CS7;
                break;
            case 6:
                old_cfg.c_cflag |= CS6;
                break;
            case 5:
                old_cfg.c_cflag |= CS5;
                break;
            case 8:
                old_cfg.c_cflag |= CS8;
                break;
        }

        switch(comport->parity)
        {
            case 1:
                old_cfg.c_cflag |= (PARENB | PARODD);
                old_cfg.c_cflag |= (INPCK | ISTRIP);
                break;
            case 2:
                old_cfg.c_cflag |= PARENB;
                old_cfg.c_cflag &= ~PARODD;
                old_cfg.c_cflag |= (INPCK | ISTRIP);
                break;
            case 0:
                old_cfg.c_cflag &= ~PARENB;
                break;
        }

        if(1 != comport->stopbit)
        {
            old_cfg.c_cflag |= CSTOPB;
        }
        else
        {
            old_cfg.c_cflag &= ~CSTOPB;
        }

        // set flow control
        // 1: software control; 2:hardware control; 0: none
        switch(comport->flowctrl)
        {
            case 1:
                old_cfg.c_cflag &= ~(CRTSCTS);
                old_cfg.c_iflag |= (IXON | IXOFF);
                break;
            case 2:
                old_cfg.c_cflag |= CRTSCTS;
                old_cfg.c_iflag &= ~(IXON | IXOFF);
                break;
            case 0:
                old_cfg.c_cflag &= ~(CRTSCTS);
                break;
        }


        // set baudRate
        cfsetispeed(&old_cfg, speed);
        cfsetospeed(&old_cfg, speed);

        new_cfg.c_cc[VMIN] = 10;
        new_cfg.c_cc[VTIME] = 0;


        // 将设置的串口参数应用到串口上，TCSANOW表示立即生效，（TCSADRAIN：在所有输出都被传输后生效；TCSAFLUSH：在所有输出都被传输后生效，同时丢弃所有未读取的输入）
        if(tcsetattr(comport->fd, TCSANOW, &old_cfg))
        {
            LOGE("tcsetattr() failed:%s", strerror(errno));
            close(comport->fd);
            return -1;
        }
        LOGD("Connected device \" %s \" successfully\n", comport->devname);
        LOGD("port:%s, databit:%d, stopbit:%d, parity:%d, flowctl:%d", comport->devname, comport->databit, comport->stopbit, comport->parity, comport->flowctrl);
    }
    return 0;
}
extern "C"
JNIEXPORT int JNICALL
Java_com_example_serial_SerialControl_closeSerialPort(JNIEnv *env, jclass clazz) {
    // TODO: implement closeSerialPort()
    if ( !comport )
    {
        LOGE("%s() get invalid input arguments.\n", __FUNCTION__ );
        return -1;
    }

    if (comport->fd >= 0)
    {
        close(comport->fd);
        LOGD("close device \" %s \" successfully\n", comport->devname);
    }
    comport->fd = -1;
    free(comport);
    comport = NULL;
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serial_SerialControl_sendToPort(JNIEnv *env, jclass clazz, jstring msg, jint len) {
    // TODO: implement sendToPort()
    // msg: 表示发送的数据；len: 发送的长度
    int   rv;
    char *ptr, left_bytes;
    int  send = 0;
    const char *sendMsg = env->GetStringUTFChars(msg, 0);
    if ( sendMsg == nullptr )
    {
        return -1;
    }

    if( !comport || !msg || len <= 0 )
    {
        LOGE("Invalid parameter.\n");
        return -1;
    }
    if (comport->fd == -1)
    {
        LOGE("Serail not connected.\n");
        return -2;
    }

    left_bytes = len;
    ptr = (char *)sendMsg;

    while( left_bytes > 0 )
    {
        /* Large data, then slice them to frag and send */
        send = left_bytes>comport->frag_size ? comport->frag_size : left_bytes;

        rv = write(comport->fd, ptr, send);
        if( rv<0 )
        {
            return -4;
        }

        left_bytes -= rv;
        ptr += rv;
    }
    LOGD("send %s successfully\n", sendMsg);

    return rv;
}


extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_serial_SerialControl_recvFromPort(JNIEnv *env, jclass clazz, jint len, jint timeout) {
    int            rv = 0;
    int            iRet;
    fd_set         rdfds, exfds;
    struct timeval stTime;

    jstring result;
    char           nativeMsg[len];
    memset(nativeMsg, 0, len);

    if( !comport  || len <= 0 )
    {
        LOGE("Invalid parameter.\n");
        return nullptr;
    }
    if (comport->fd == -1)
    {
        LOGE("Serail not connected.\n");
        return nullptr;
    }

    FD_ZERO(&rdfds);
    FD_ZERO(&exfds);
    FD_SET(comport->fd, &rdfds);
    FD_SET(comport->fd, &exfds);

    if (0xFFFFFFFF != timeout)
    {
        stTime.tv_sec = (time_t) (timeout / 1000);
        stTime.tv_usec = (long)(1000 * (timeout % 1000));

        iRet = select(comport->fd + 1, &rdfds, 0, &exfds, &stTime);
        if (0 == iRet)
        {
            return nullptr;
        }
        else if (0 < iRet)
        {
            if (0 != FD_ISSET(comport->fd, &exfds))
            {
                LOGE("Error checking recv status.\n");
                return nullptr;
            }

            if (0 == FD_ISSET(comport->fd, &rdfds))
            {
                LOGE("No incoming data.\n");
                return nullptr;
            }
        }
        else
        {
            if (EINTR == errno)
            {
                LOGE("catch interrupt signal.\n");
//                rv = 0;
            }
            else
            {
                LOGE("Check recv status failure.\n");
//                rv = -7;
            }
            return nullptr;
        }
    }

    usleep(10000); /* sleep for 10ms for data incoming */

    // Get data from Com port
    iRet = read(comport->fd, nativeMsg, len);
    if (0 > iRet)
    {
        if (EINTR == errno)
            rv = 0;      // Interrupted signal catched
        else
            rv = -3;      // Failed to read Comport

        return nullptr;
    }
    LOGD("Receive {%s} successfully\n", nativeMsg);

    return env->NewStringUTF(nativeMsg);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_serial_SerialControl_saveToFile(JNIEnv *env, jclass clazz, jstring msg, jint len,
                                                 jstring file_name) {
    // TODO: implement saveToFile()
    int mFd = -1;
    int rv;
    mFd = open("dev/log.txt", O_CREAT | O_RDWR | O_APPEND, 0666);
    rv = write(mFd, msg, len);
    if (rv < 0) {
        LOGE("Save data to file failed:%s\n" , strerror(errno));
        return nullptr;
    }
    LOGD("save received data into file successfully!\n");
    char buf[1024];
    rv = read(mFd, buf, len);
    if (rv < 0) {
        LOGE("Read data failed:%s\n" , strerror(errno));
        return nullptr;
    }
    strcat(reinterpret_cast<char *>(buf), "file");
    close(mFd);
    return reinterpret_cast<jstring>(buf);
}

// RS485是半双工通信的，由GPIO的高低电平来控制其是接收状态还是发送状态
// 0: 接收（默认）；1：发送
// 切换状态成功返回0，失败返回-1
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_serial_SerialControl_changeState(JNIEnv *env, jclass clazz, jint state) {
    // TODO: implement changeState()
    struct gpiod_chip  *chip;
    struct gpiod_line  *line;
    const char         *chipname = "gpiochip4";
    int                 rv = 0;
    int                 gpio = 26;


    chip = gpiod_chip_open_by_name(chipname);
    if( !chip )
    {
        LOGE("Gpio open '%s' failed:%s\n", chipname, strerror(errno));
        return -1;
    }

    line = gpiod_chip_get_line(chip, gpio);
    if ( !line )
    {
        LOGE("Get gpio[%d] failed:%s\n", gpio, strerror(errno));
        rv = -1;
        goto Cleanup;
    }

    rv = gpiod_line_is_used(line);
    if( rv )
    {
        LOGE("GPIO[%d] is used\n", gpio);
        rv = -1;
        goto Cleanup;
    }

    rv = gpiod_line_request_output(line, "rs485", 0);
    if ( rv < 0 )
    {
        LOGE("Set GPIO[%d] as output failed:%s\n", gpio, strerror(errno));
        rv = -1;
        goto Cleanup;
    }

    rv = gpiod_line_set_value(line, state);
    if ( rv < 0 )
    {
        LOGE("Set GPIO[%d]'s value failed:%s\n", gpio, strerror(errno));
        rv = -1;
        goto Cleanup;
    }
    LOGD("Set GPIO[%d]'s value as %d successfully\n", gpio, state);
    rv = 0;

Cleanup:
    gpiod_line_release(line);
    gpiod_chip_close(chip);
    return rv;
}