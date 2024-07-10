import React from 'react';
import {
  DeviceEventEmitter,
  EmitterSubscription,
  findNodeHandle,
  NativeModules,
  Platform,
  requireNativeComponent,
  ViewStyle,
} from 'react-native';

// Definição do componente nativo
const Scanner = requireNativeComponent<ScannerProps>('RNScanner');
const ScannerManager: any = NativeModules.RNScannerManager;

export interface PictureCallbackProps {
  croppedImage: string;
  initialImage: string;
}

export interface Coordinate {
  x: number;
  y: number;
}

export interface RectangleProps {
  bottomLeft: Coordinate;
  bottomRight: Coordinate;
  topLeft: Coordinate;
  topRight: Coordinate;
}

export interface DetectedRectangle extends RectangleProps {
  dimensions: {
    height: number;
    width: number;
  };
}

export interface PictureTaken {
  initialImage: string;
  width?: number;
  height?: number;
  rectangleCoordinates?: RectangleProps;
}

export interface DeviceSetupCallbackProps {
  hasCamera: boolean;
  permissionToUseCamera: boolean;
  flashIsAvailable: boolean;
  previewHeightPercent: number;
  previewWidthPercent: number;
}

export interface TorchCallbackProps {
  enabled: boolean;
}

export interface Filter {
  id: number;
  name: string;
}

export interface AndroidPermissionObject {
  title: string;
  message: string;
  buttonNegative: string;
  buttonPositive: string;
}

interface ScannerProps {
  start?: () => void;
  stop?: () => void;
  focus?: () => void;
  cleanup?: () => void;
  capture?: () => void;
  onRectangleDetect?: (event: any) => void;
  onProcessing?: () => void;
  onPictureProcessed?: (args: PictureCallbackProps) => void;
  onPictureTaken?: (event: any) => void;
  onDeviceSetup?: (args: DeviceSetupCallbackProps) => void;
  onRectangleDetected?: (args: { detectedRectangle: DetectedRectangle }) => void;
  onTorchChanged?: (args: TorchCallbackProps) => void;
  onErrorProcessingImage?: (args: PictureCallbackProps) => void;
  filterId?: number;
  capturedQuality?: number;
  styles?: object;
  androidPermission?: AndroidPermissionObject | boolean;
  quality?: number;
  overlayColor?: number | string;
  enableTorch?: boolean;
  saveOnDevice?: boolean;
  useFrontCam?: boolean;
  saturation?: number;
  brightness?: number;
  contrast?: number;
  detectionCountBeforeCapture?: number;
  durationBetweenCaptures?: number;
  detectionRefreshRateInMS?: number;
  documentAnimation?: boolean;
  noGrayScale?: boolean;
  manualOnly?: boolean;
  style?: ViewStyle;
  useBase64?: boolean;
  saveInAppDocument?: boolean;
  captureMultiple?: boolean;
  showBorder?: boolean;
  faceDetection?: boolean;
  cropHeight?: boolean;
  cropWidth?: boolean;
  doublePageScan?: boolean; // book scan
}

class ScannerComponent extends React.Component<ScannerProps> {
  private onPictureTaken: EmitterSubscription | undefined;
  private onProcessingChange: EmitterSubscription | undefined;

  sendOnPictureTakenEvent(event: any) {
    if (!this.props.onPictureTaken) return null;
    return this.props.onPictureTaken(event.nativeEvent);
  }

  sendOnRectangleDetectEvent(event: any) {
    if (!this.props.onRectangleDetect) return null;
    return this.props.onRectangleDetect(event.nativeEvent);
  }

  getImageQuality() {
    if (!this.props.quality) return 0.8;
    if (this.props.quality > 1) return 1;
    if (this.props.quality < 0.1) return 0.1;
    return this.props.quality;
  }

  componentDidMount() {
    if (Platform.OS === 'android') {
      const { onPictureTaken, onProcessing } = this.props;
      if (onPictureTaken) {
        this.onPictureTaken = DeviceEventEmitter.addListener('onPictureTaken', onPictureTaken);
      }
      if (onProcessing) {
        this.onProcessingChange = DeviceEventEmitter.addListener('onProcessingChange', onProcessing);
      }
    }
  }

  componentDidUpdate(prevProps: ScannerProps) {
    if (Platform.OS === 'android') {
      if (this.props.onPictureTaken !== prevProps.onPictureTaken) {
        if (prevProps.onPictureTaken) {
          this.onPictureTaken && this.onPictureTaken.remove();
        }
        if (this.props.onPictureTaken) {
          this.onPictureTaken = DeviceEventEmitter.addListener(
            'onPictureTaken',
            this.props.onPictureTaken
          );
        }
      }
      if (this.props.onProcessing !== prevProps.onProcessing) {
        if (prevProps.onProcessing) {
          this.onProcessingChange && this.onProcessingChange.remove();
        }
        if (this.props.onProcessing) {
          this.onProcessingChange = DeviceEventEmitter.addListener(
            'onProcessingChange',
            this.props.onProcessing
          );
        }
      }
    }
  }

  componentWillUnmount() {
    if (Platform.OS === 'android') {
      const { onPictureTaken, onProcessing } = this.props;
      if (onPictureTaken) this.onPictureTaken && this.onPictureTaken.remove();
      if (onProcessing) this.onProcessingChange && this.onProcessingChange.remove();
    }
  }

  capture() {
    if (this._scannerHandle) {
      ScannerManager.capture();
    }
  }

  _scannerRef: any = null;
  _scannerHandle: number | null = null;
  _setReference = (ref: any) => {
    if (ref) {
      this._scannerRef = ref;
      this._scannerHandle = findNodeHandle(ref);
    } else {
      this._scannerRef = null;
      this._scannerHandle = null;
    }
  };

  render() {
    return (
      <Scanner
        ref={this._setReference}
        {...this.props}
        onPictureTaken={this.sendOnPictureTakenEvent.bind(this)}
        onRectangleDetect={this.sendOnRectangleDetectEvent.bind(this)}
        useFrontCam={this.props.useFrontCam || false}
        brightness={this.props.brightness || 0}
        saturation={this.props.saturation || 1}
        contrast={this.props.contrast || 1}
        quality={this.getImageQuality()}
        detectionCountBeforeCapture={this.props.detectionCountBeforeCapture || 5}
        durationBetweenCaptures={this.props.durationBetweenCaptures || 0}
        detectionRefreshRateInMS={this.props.detectionRefreshRateInMS || 50}
      />
    );
  }
}

export default ScannerComponent;
export { ScannerManager };
