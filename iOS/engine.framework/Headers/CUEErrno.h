#import <Foundation/Foundation.h>

typedef NS_ENUM(NSInteger, CUE_ENGINE_ERROR) {
    CUE_ENGINE_SUCCESS = 0,
    
    CUE_ENGINE_ERR_QUEUE_TRIGGER_NO_ACTIVE_TRANSMITTER = -1,
    CUE_ENGINE_ERR_QUEUE_MESSAGE_NO_ACTIVE_TRANSMITTER = -2,
    CUE_ENGINE_ERR_NUMBER_OF_SYMBOLS_MISMATCH = -3,
    CUE_ENGINE_ERR_SYMBOL_NOT_A_NUMBER = -5,
    CUE_ENGINE_ERR_NO_ACTIVE_TRANSMITTER = -6,
    CUE_ENGINE_ERR_TRIGGER_AS_NUMBER_MAX_NUMBER_EXCEEDED = -7,
    CUE_ENGINE_ERR_INDEX_VALUE_EXCEEDED = -8,

    CUE_ENGINE_ERR_G1_TRIGGER_AS_NUMBER_UNSUPPORTED = -101,
    CUE_ENGINE_ERR_G1_NUMBER_OF_SYMBOLS_EXCEEDED = -102,

    CUE_ENGINE_ERR_G2_QUEUE_MESSAGE_STRING_SIZE_IN_BYTES_EXCEEDED = -201,
    CUE_ENGINE_ERR_G2_QUEUE_LIVE_UNSUPPORTED = -202,
    CUE_ENGINE_ERR_G2_QUEUE_DATA_UNSUPPORTED = -203
};
