#import <Cordova/CDVPlugin.h>
#import "QBImagePickerController.h"

@interface MediaPicker : CDVPlugin <QBImagePickerControllerDelegate>

- (void)getPictures:(CDVInvokedUrlCommand *)command;

@end
