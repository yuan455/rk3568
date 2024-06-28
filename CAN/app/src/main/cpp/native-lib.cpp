#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <sys/socket.h>
#include <linux/can.h>
#include <linux/can/raw.h>
#include <android/log.h>

#define TAG "CanControl"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_test_1can_CanControl_sendMessage(JNIEnv *env, jclass clazz, jstring id,
                                                  jstring dlc, jstring data,
                                                  jstring ifname) {
    // TODO: implement sendMessage()
    int                  fd;
    int                  i;
    struct sockaddr_can  addr;
    struct ifreq         ifr;
    struct can_frame     frame;
    unsigned int         byte;
    char                *token;

    const char *sendId = env->GetStringUTFChars(id, 0);
    const char *sendDlc = env->GetStringUTFChars(dlc, 0);
    const char *sendMsg = env->GetStringUTFChars(data, 0);
    const char *if_name = env->GetStringUTFChars(ifname, 0);
    if ( sendId == nullptr || sendDlc == nullptr || sendMsg == nullptr || if_name == nullptr)
    {
        LOGE("Invalid parameter");
        return -1;
    }

    // create socket
    fd = socket(PF_CAN, SOCK_RAW, CAN_RAW);
    if(fd < 0)
    {
        LOGE("create can socket failed:%s\n", strerror(errno));
        return -2;
    }

    strcpy(ifr.ifr_name, if_name);
    ioctl(fd, SIOCGIFINDEX, &ifr);
    addr.can_family = AF_CAN;
    addr.can_ifindex = ifr.ifr_ifindex;

    if (bind(fd, (struct sockaddr*)&addr, sizeof (addr)) < 0)
    {
        LOGE("bind socket failed:%s\n", strerror(errno));
        return -3;
    }
    sscanf(sendId, "%X", &byte);
    frame.can_id = byte;
    sscanf(sendDlc, "%u", &byte);
    frame.can_dlc = byte;
    token = strtok((char *)sendMsg, " ");
    for (int i = 0; i < frame.can_dlc && token != NULL; ++i)
    {
        unsigned int data_byte;
        sscanf(token, "%02X", &data_byte);
        frame.data[i] = (unsigned char)data_byte;
        token = strtok(NULL, " ");
    }

    if (write(fd, &frame, sizeof (struct can_frame)) != sizeof (struct can_frame))
    {
        LOGE("write data failed:%s\n", strerror(errno));
        return -4;
    }

    LOGD("Send CAN frame: ID=0x%X DLC=%d data=", frame.can_id, frame.can_dlc);
    for(int i = 0; i < frame.can_dlc; i++)
    {
        LOGD("%02X ", frame.data[i]);
    }
    printf("\n");

    close(fd);
    return 0;
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_test_1can_CanControl_receiveMessage(JNIEnv *env, jclass clazz, jstring ifname) {
    // TODO: implement receiveMessage()
    int                  fd;
    int                  rv;
    struct sockaddr_can  addr;
    struct ifreq         ifr;
    struct can_frame     frame;
    char                 recvMsg[128];
    char                 buf[4];

    const char *if_name = env->GetStringUTFChars(ifname, 0);
    if (if_name == nullptr)
    {
        LOGE("Invalid parameter");
        return nullptr;
    }

    fd = socket(PF_CAN, SOCK_RAW, CAN_RAW);
    if(fd < 0)
    {
        LOGE("create can socket failed:%s\n", strerror(errno));
        return nullptr;
    }
    strcpy(ifr.ifr_name, if_name);
    ioctl(fd, SIOCGIFINDEX, &ifr);
    addr.can_family = AF_CAN;
    addr.can_ifindex = ifr.ifr_ifindex;

    if (bind(fd, (struct sockaddr*)&addr, sizeof (addr)) < 0)
    {
        LOGE("bind socket failed:%s\n", strerror(errno));
        return nullptr;
    }

    rv = read(fd, &frame, sizeof (struct can_frame));
    if (rv < 0)
    {
        LOGE("read data failed:%s\n", strerror(errno));
        return nullptr;
    } else if (rv < sizeof (struct can_frame))
    {
        LOGE("read incomplete CAN frame\n");
        return nullptr;
    }
    memset(recvMsg, 0, sizeof (recvMsg));
    sprintf(recvMsg, "ID=%X DLC=%d Data=", frame.can_id, frame.can_dlc);
    for (int i = 0; i < frame.can_dlc; ++i) {
        memset(buf, 0, sizeof (buf));
        sprintf(buf, "%02X ", frame.data[i]);
        strcat(recvMsg, buf);
    }
    LOGD("recived: %s", recvMsg);
    close(fd);
    return env->NewStringUTF(recvMsg);
}