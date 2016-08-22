
"use strict";

import { DeviceEventEmitter, NativeModules } from 'react-native';
import { EventEmitter } from 'events';

const { Meglive } = NativeModules;

// Event emitter to dispatch request and response from Meglive.
const emitter = new EventEmitter();

DeviceEventEmitter.addListener('onDetectingResult', resp => {
  emitter.emit('Meglive_onDetectingResult', resp);
});

/**
 * 启动活体认证
 */
export function detecting() {
  Meglive.detecting();
}

