#import <Cordova/CDV.h>

#import <Foundation/Foundation.h>
@import UIKit;
#import <MapKit/MapKit.h>
#import <CoreLocation/CoreLocation.h>
#import <Cordova/CDVPlugin.h>
#import <UIKit/UIKit.h>

@interface HWPHello : CDVPlugin <CLLocationManagerDelegate, UIImagePickerControllerDelegate, UINavigationControllerDelegate, UIAlertViewDelegate>
{
    UIImagePickerController *ipc;
    UIPopoverController *popover;
}
- (void)StartService:(CDVInvokedUrlCommand*)command;
- (void)SendLocation:(CDVInvokedUrlCommand*)command;
- (void)GetLocation:(CDVInvokedUrlCommand*)command;
- (void)timeReader:(CDVInvokedUrlCommand*)command;
- (void)CheckSumIndicatorResult:(CDVInvokedUrlCommand*)command;
- (void)CheckLocation:(CDVInvokedUrlCommand*)command;
- (void)DespatchQueue:(CDVInvokedUrlCommand*)command;
- (void)CopyFileFromPath:(NSString *)source toDestination:(NSString *)destination;
- (void)copyFileExample:(CDVInvokedUrlCommand*)command;
- (void)FileChooser:(CDVInvokedUrlCommand*)command;
- (NSString *)contentTypeForImageData:(NSData *)data;
- (void)UpdateChoice:(CDVInvokedUrlCommand*)command;
- (void)UpdateConfirm:(CDVInvokedUrlCommand*)command;
@property (strong, nonatomic) CLLocationManager * locationManager;
@property (strong, nonatomic) NSMutableArray * coordinatesArray;
@property (weak, nonatomic) IBOutlet UIButton *btnGallery;
@property (strong, nonatomic) UIViewController * viewcont;
@property (nonatomic, strong) NSString *callbackIdForImagePicker;
@property (nonatomic, strong) NSString *callbackIdForAppUpdate;
@end
