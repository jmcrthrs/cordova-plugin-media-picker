//
// MultipleMediaSelection.h
//

#import <Cordova/CDVPlugin.h>
#import "QBImagePickerController.h"

@interface MultipleMediaSelection : CDVPlugin <QBImagePickerControllerDelegate>

- (void) getPictures:(CDVInvokedUrlCommand *)command;

@end
