import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';
import { tts, getVoices } from 'edge-tts/out/index.js';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

// Enable JSON parsing for POST requests
app.use(express.json());

/**
 * Example Express Rest API endpoints can be defined here.
 * Uncomment and define endpoints as necessary.
 *
 * Example:
 * ```ts
 * app.get('/api/{*splat}', (req, res) => {
 *   // Handle API request
 * });
 * ```
 */

/**
 * Edge TTS API for Cantonese text-to-speech
 * POST /api/tts/cantonese
 * Body: { text: string, voice?: string, rate?: string, volume?: string, pitch?: string }
 * Returns: Audio file (audio/mpeg)
 */
app.post('/api/tts/cantonese', async (req, res) => {
  try {
    const { text, voice, rate, volume, pitch } = req.body;

    if (!text) {
      res.status(400).json({ error: 'Text parameter is required' });
      return;
    }

    // Cantonese voices available in Edge TTS:
    // zh-HK-HiuMaanNeural (Female)
    // zh-HK-HiuGaaiNeural (Female)
    // zh-HK-WanLungNeural (Male)
    const selectedVoice = voice || 'zh-HK-HiuMaanNeural';

    // Generate speech with edge-tts
    const audioBuffer = await tts(text, {
      voice: selectedVoice,
      rate: rate || '+0%',
      volume: volume || '+0%',
      pitch: pitch || '+0Hz'
    });

    // Set appropriate headers for audio streaming
    res.setHeader('Content-Type', 'audio/mpeg');
    res.setHeader('Content-Disposition', 'inline; filename="cantonese-tts.mp3"');
    res.setHeader('Content-Length', audioBuffer.length.toString());

    // Send the audio buffer
    res.send(Buffer.from(audioBuffer));
  } catch (error) {
    console.error('Edge TTS error:', error);
    res.status(500).json({ 
      error: 'Failed to generate speech',
      details: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * Get available Cantonese voices
 * GET /api/tts/cantonese/voices
 */
app.get('/api/tts/cantonese/voices', async (req, res) => {
  try {
    // Get all available voices
    const allVoices = await getVoices();
    
    // Filter for Cantonese (Hong Kong) voices
    const cantoneseVoices = allVoices.filter(v => v.Locale.startsWith('zh-HK'));

    res.json({ 
      voices: cantoneseVoices.map(v => ({
        name: v.Name,
        shortName: v.ShortName,
        friendlyName: v.FriendlyName,
        gender: v.Gender,
        locale: v.Locale
      }))
    });
  } catch (error) {
    console.error('Error fetching voices:', error);
    res.status(500).json({ 
      error: 'Failed to fetch voices',
      details: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url)) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
