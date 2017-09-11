#import <Cordova/CDVPlugin.h>
#import "QBImagePickerController.h"

@interface MediaPicker : CDVPlugin <QBImagePickerControllerDelegate>

- (void)cleanUp:(CDVInvokedUrlCommand *)command;
- (void)getPictures:(CDVInvokedUrlCommand *)command;

@end
