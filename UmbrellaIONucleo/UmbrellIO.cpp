/*
IoT Hackaton Code for the UmbrellIO Project,
 
Code is based on (C)2015 Semtech LoRaMac classA device implementation
*/
#include "mbed.h"
#include "board.h"
#include "LoRaMac.h"
#include "utilities.h"
#include "ChainableLED.h"
#include "GPS.h"
 
/*!
* When set to 1 the application uses the Over-the-Air activation procedure
* When set to 0 the application uses the Personalization activation procedure
*/
#define OVER_THE_AIR_ACTIVATION                     0
 
/*!
* Mote device IEEE EUI
*/
static uint8_t DevEui[] =
{
    0xF0, 0x3D, 0x29, 0x10, 0x00, 0x00, 0x16, 0x2C                  /// <<< Change here
};
 
/*!
* Device address
*/
static uint32_t DevAddr = 0x8000162C;                             
 
/*!
* AES encryption/decryption cipher network session key
*/
static uint8_t NwkSKey[] =
{
    0x11, 0xa7, 0x8e, 0x50, 0xbb, 0xf1, 0xf5, 0xdf,                
    0x95, 0x9f, 0x2b, 0xe0, 0x61, 0xd3, 0x3b, 0x4e
};
 
 
/*!
* AES encryption/decryption cipher application session key
*/
static uint8_t AppSKey[] =
{
    0x97, 0x74, 0x11, 0x8b, 0xa8, 0xd8, 0x22, 0x1c,                
    0x65, 0x8f, 0x70, 0xb8, 0x0e, 0x55, 0xbd, 0x1c
};
 
/*!
* Indicates if the MAC layer has already joined a network.
*/
static bool IsNetworkJoined = false;
 
/*!
* Defines the application data transmission duty cycle
*/
#define APP_TX_DUTYCYCLE                              5000000  // 20 [s] value in us (minimum 10s)
#define APP_TX_DUTYCYCLE_RND                          1000000  // 5 [s] value in us (minimum 3s)
 
/*!
* User application data buffer size
*/
#define APP_DATA_SIZE                               6
 
/*!
* User application data
*/
static uint8_t AppData[APP_DATA_SIZE];
 
/*!
* Defines the application data transmission duty cycle
*/
static uint32_t TxDutyCycleTime;
 
Ticker TxNextPacketTimer;
 
#if( OVER_THE_AIR_ACTIVATION != 0 )
 
/*!
* Defines the join request timer
*/
Ticker JoinReqTimer;
 
#endif
 
/*!
* Indicates if a new packet can be sent
*/
static bool TxNextPacket = true;
static bool TxDone = false;
 
static uint8_t AppPort = 3;
static uint8_t AppDataSize = APP_DATA_SIZE;
 
static LoRaMacEvent_t LoRaMacEvents;
 
GPS gps(D1, D0);
ChainableLED color_led(D8, D9, 3);
 
const int32_t MaxNorthPosition = 8388607;       // 2^23 - 1
const int32_t MaxSouthPosition = 8388608;       // -2^23
const int32_t MaxEastPosition = 8388607;        // 2^23 - 1   
const int32_t MaxWestPosition = 8388608;        // -2^23
int32_t LatitudeBinary, LongitudeBinary = 0;
float Latitude, Longitude;
 
 
void GpsConvertPositionIntoBinary( void )
{
    long double temp;
 
    if( Latitude >= 0 ) // North
    {   
        temp = Latitude * MaxNorthPosition;
        LatitudeBinary = temp / 90;
    }
    else                // South
    {   
        temp = Latitude * MaxSouthPosition;
        LatitudeBinary = temp / 90;
    }
 
    if( Longitude >= 0 ) // East
    {   
        temp = Longitude * MaxEastPosition;
        LongitudeBinary = temp / 180;
    }
    else                // West
    {    
        temp = Longitude * MaxWestPosition;
        LongitudeBinary = temp / 180;
    }
}
 
/*!
*
*/
static void PrepareTxFrame( uint8_t port )
{   
    AppData[0] = ( LatitudeBinary >> 16 ) & 0xFF;
    AppData[1] = ( LatitudeBinary >> 8 ) & 0xFF;
    AppData[2] = LatitudeBinary & 0xFF;
    AppData[3] = ( LongitudeBinary >> 16 ) & 0xFF;
    AppData[4] = ( LongitudeBinary >> 8 ) & 0xFF;
    AppData[5] = LongitudeBinary & 0xFF;
}
 
static void ProcessRxFrame( LoRaMacEventFlags_t *flags, LoRaMacEventInfo_t *info )
{
}
 
static bool SendFrame( void )
{
    uint8_t sendFrameStatus = 0;
 
    sendFrameStatus = LoRaMacSendFrame( AppPort, AppData, AppDataSize );
//   sendFrameStatus = LoRaMacSendConfirmedFrame( AppPort, AppData, AppDataSize, 8 );
    switch( sendFrameStatus )
    {
    case 5: // NO_FREE_CHANNEL
        // Try again later
        return true;
    default:
        return false;
    }
}
 
 
#if( OVER_THE_AIR_ACTIVATION != 0 )
/*!
* \brief Function executed on JoinReq Timeout event
*/
static void OnJoinReqTimerEvent( void )
{
    TxNextPacket = true;
    JoinReqTimer.detach( );
}
#endif
 
 
/*!
* \brief Function executed on TxNextPacket Timeout event
*/
static void OnTxNextPacketTimerEvent( void )
{   
    TxNextPacket = true;
    TxNextPacketTimer.detach( );
}
 
 
/*!
* \brief Function to be executed on MAC layer event
*/
static void OnMacEvent( LoRaMacEventFlags_t *flags, LoRaMacEventInfo_t *info )
{
    if( flags->Bits.JoinAccept == 1 )
    {
#if( OVER_THE_AIR_ACTIVATION != 0 )
        JoinReqTimer.detach( );
#endif
        IsNetworkJoined = true;
    }
   
    if( flags->Bits.Tx == 1 )
    {
    }
 
    if( flags->Bits.Rx == 1 )
    {
        if( flags->Bits.RxData == true )
        {
            ProcessRxFrame( flags, info );
        }
    }
 
    // Schedule a new transmission
    TxDone = true;
}
 
/**
* Main application entry point.
*/
int main( void )
{
   
#if( OVER_THE_AIR_ACTIVATION != 0 )
    uint8_t sendFrameStatus = 0;
#endif
    bool trySendingFrameAgain = false;
 
    debug( "\n\n\r    LoRaWAN Class A Demo code  \n\n\r" );
   
    BoardInitMcu( );
    BoardInitPeriph( );
 
    // Initialize LoRaMac device unique ID
    // BoardGetUniqueId( DevEui );
   
    LoRaMacEvents.MacEvent = OnMacEvent;
    LoRaMacInit( &LoRaMacEvents );
 
    IsNetworkJoined = false;
 
#if( OVER_THE_AIR_ACTIVATION == 0 )
    // Random seed initialization
    srand( RAND_SEED );
    // Choose a random device address
    // NwkID = 0
    // NwkAddr rand [0, 33554431]
    if( ( DevAddr == 0 ) || ( DevAddr == 0xFFFFFFFF ) )
    {
        // Generate random DevAddr if it does not exist
        debug("Generate random DevAddr\n\r");
        DevAddr = randr( 0, 0x01FFFFFF );
    }
    debug( "- DevAddr = 0x%x\n\r" , DevAddr);   
    LoRaMacInitNwkIds( 0x000000, DevAddr, NwkSKey, AppSKey );
    IsNetworkJoined = true;
#endif
 
    TxNextPacket = true;
 
    LoRaMacSetAdrOn( false );
   
    LoRaMacSetDutyCycleOn( false );   
                  
    while( 1 )
    { 
        if(gps.sample())
        {
            if( gps.longitude != 0.0 || gps.latitude != 0.0 )
            {
                Longitude = gps.longitude;
                Latitude = gps.latitude;
               
                GpsConvertPositionIntoBinary();
                
                 color_led.setColorRGB( 0, ( uint8_t )( 0 ), ( uint8_t )( 255 ), ( uint8_t )( 0 ) );
            }  
            else
                color_led.setColorRGB( 0, ( uint8_t )( 255 ), ( uint8_t )( 0 ), ( uint8_t )( 0 ) );
        }
    
        while( IsNetworkJoined == false )
        {
#if( OVER_THE_AIR_ACTIVATION != 0 )
            if( TxNextPacket == true )
            {
                TxNextPacket = false;
               
                sendFrameStatus = LoRaMacJoinReq( DevEui, AppEui, AppKey );
                debug("Req Sent\n\r");
                switch( sendFrameStatus )
                {
                case 1: // BUSY
                    break;
                case 0: // OK
                case 2: // NO_NETWORK_JOINED
                case 3: // LENGTH_PORT_ERROR
                case 4: // MAC_CMD_ERROR
                case 6: // DEVICE_OFF
                default:
                    // Relaunch timer for next trial
                    JoinReqTimer.attach_us( &OnJoinReqTimerEvent, OVER_THE_AIR_ACTIVATION_DUTYCYCLE );
                    break;
                }
            }
//            TimerLowPowerHandler( );
#endif
        }
 
        if( TxDone == true )
        {
           
            TxDone = false;
           
            debug( "TxDone \n\n\r" );
            // Schedule next packet transmission
            TxDutyCycleTime = APP_TX_DUTYCYCLE + randr( -APP_TX_DUTYCYCLE_RND, APP_TX_DUTYCYCLE_RND );
            TxNextPacketTimer.attach_us( &OnTxNextPacketTimerEvent, TxDutyCycleTime );
        }
 
        if( trySendingFrameAgain == true )
        {
            trySendingFrameAgain = SendFrame( );
        }
       
        if( TxNextPacket == true )
        {      
            TxNextPacketTimer.detach( );
            
            TxNextPacket = false;
       
            PrepareTxFrame( AppPort );
           
            trySendingFrameAgain = SendFrame( );
        }
       
    }
}