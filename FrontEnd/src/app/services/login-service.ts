import {Injectable} from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class LoginService {

    async createPasskey(username: string): Promise<string | void> {
        const challenge = new Uint8Array(32);
        crypto.getRandomValues(challenge);

        const hostname = window.location.hostname;
        const rp: PublicKeyCredentialRpEntity = hostname === 'localhost' || /^\d+\.\d+\.\d+\.\d+$/.test(hostname)
            ? {name: 'ACME Corporation'}
            : {id: hostname, name: 'ACME Corporation'};

        const userId = new Uint8Array(16);
        crypto.getRandomValues(userId);

        const publicKey: PublicKeyCredentialCreationOptions = {
            challenge,
            rp,
            user: {
                id: userId,
                name: username || 'jamiedoe',
                displayName: username || 'Jamie Doe',
            },
            pubKeyCredParams: [{type: "public-key", alg: -7}],
            timeout: 60000,
            authenticatorSelection: {userVerification: 'preferred'},
            attestation: 'none'
        };

        try {
            if (!('credentials' in navigator) || typeof navigator.credentials.create !== 'function') {
                return '浏览器不支持 WebAuthn';
            }
            const publicKeyCredential = await navigator.credentials.create({publicKey}) as PublicKeyCredential | null;
            if (!publicKeyCredential) {
                return '未返回凭证';
            }
            console.log('PublicKeyCredential:', publicKeyCredential);
        } catch (err) {
            console.error(err);
            const name = (err as DOMException).name;
            if (name === 'OperationError' || name === 'InvalidStateError') {
                return '已有未完成的请求或操作被取消。';
            } else if (name === 'SecurityError') {
                return 'RP ID 与当前域不匹配，请使用与站点相同的可注册域名或在本地使用 localhost。';
            } else {
                return String(err);
            }
        }
    }

    async getAssertion(): Promise<string | void> {
        const challenge = new Uint8Array(32);
        crypto.getRandomValues(challenge);

        const hostname = window.location.hostname;
        const rpId = hostname === 'localhost' || /^\d+\.\d+\.\d+\.\d+$/.test(hostname) ? undefined : hostname;

        const publicKey: PublicKeyCredentialRequestOptions = {
            challenge,
            timeout: 60000,
            userVerification: 'preferred',
        };

        if (rpId) (publicKey as any).rpId = rpId;

        try {
            if (!('credentials' in navigator) || typeof navigator.credentials.get !== 'function') {
                return '浏览器不支持 WebAuthn';
            }
            const credential = await navigator.credentials.get({publicKey}) as PublicKeyCredential | null;
            if (!credential) {
                return '未返回凭证';
            }
            console.log('PublicKeyCredential (assertion):', credential);

            const authResp = credential.response as AuthenticatorAssertionResponse;
            const toBase64Url = (buf: ArrayBuffer | Uint8Array) => {
                const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
                const str = String.fromCharCode(...bytes);
                return btoa(str).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
            };

            const payload = {
                id: credential.id,
                rawId: toBase64Url(credential.rawId),
                type: credential.type,
                response: {
                    clientDataJSON: toBase64Url(authResp.clientDataJSON),
                    authenticatorData: toBase64Url(authResp.authenticatorData),
                    signature: toBase64Url(authResp.signature),
                    userHandle: authResp.userHandle ? toBase64Url(authResp.userHandle) : null
                }
            };

            console.log('Assertion payload (send to server):', payload);
            // TODO: 将 payload 发送到后端进行验证
        } catch (err) {
            console.error(err);
            const name = (err as DOMException).name;
            if (name === 'NotAllowedError' || name === 'InvalidStateError') {
                return '操作被拒绝或已有未完成的请求。';
            } else if (name === 'SecurityError') {
                return 'RP ID 与当前域不匹配，请使用与站点相同的可注册域名或在本地使用 localhost。';
            } else {
                return String(err);
            }
        }
    }
}
