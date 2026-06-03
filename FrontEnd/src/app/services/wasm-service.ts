import {Injectable} from '@angular/core';

@Injectable({
    providedIn: 'root'
})

export class WasmService {
    private wasmInstance: any;

    async loadWasm(url: string) {
        const response = await fetch(url);
        const bytes = await response.arrayBuffer();
        const result = await WebAssembly.instantiate(bytes);
        this.wasmInstance = result.instance;
    }

    callWasmFunction(arg: number): number {
        return this.wasmInstance.exports.yourFunction(arg);
    }
}
