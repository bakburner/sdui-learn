import * as Ably from 'ably';

const ABLY_TOKEN_URL = '/ably-token';

let client: Ably.Realtime | null = null;
const activeChannels = new Map<string, Ably.RealtimeChannel>();
const connectionListeners = new Set<(state: string) => void>();

async function fetchAblyToken(): Promise<string> {
  const res = await fetch(ABLY_TOKEN_URL, { headers: { Accept: 'application/json' } });
  if (!res.ok) throw new Error(`Token endpoint returned ${res.status}`);
  const json = await res.json();
  const jwt = json?.data?.accessToken;
  if (typeof jwt === 'string' && jwt.length > 0) return jwt;
  throw new Error('No accessToken found in token response');
}

function getClient(): Ably.Realtime {
  if (!client) {
    client = new Ably.Realtime({
      authCallback: async (_data, callback) => {
        try {
          const token = await fetchAblyToken();
          callback(null, token);
        } catch (err) {
          callback(err as Error, null);
        }
      },
      autoConnect: true,
    });

    client.connection.on('connected', () => {
      console.log('[Ably] Connected');
      connectionListeners.forEach((cb) => cb('connected'));
    });
    client.connection.on('disconnected', () => {
      console.warn('[Ably] Disconnected');
      connectionListeners.forEach((cb) => cb('disconnected'));
    });
    client.connection.on('suspended', () => {
      console.warn('[Ably] Suspended');
      connectionListeners.forEach((cb) => cb('suspended'));
    });
    client.connection.on('failed', (err) => {
      console.error('[Ably] Connection failed', err);
      connectionListeners.forEach((cb) => cb('failed'));
    });
  }
  return client;
}

export type MessageCallback = (data: Record<string, unknown>) => void;

/**
 * Subscribe to an Ably channel and receive parsed JSON messages.
 * Returns an unsubscribe function.
 */
export function subscribeToChannel(channelName: string, onMessage: MessageCallback): () => void {
  const ably = getClient();
  const channel = ably.channels.get(channelName);
  activeChannels.set(channelName, channel);

  const listener = (message: Ably.Message) => {
    try {
      const data = typeof message.data === 'string' ? JSON.parse(message.data) : message.data;
      if (data && typeof data === 'object') {
        onMessage(data as Record<string, unknown>);
      }
    } catch (err) {
      console.error(`[Ably] Failed to parse message on ${channelName}:`, err);
    }
  };

  channel.subscribe(listener);
  console.log(`[Ably] Subscribed to ${channelName}`);

  return () => {
    channel.unsubscribe(listener);
    activeChannels.delete(channelName);
    console.log(`[Ably] Unsubscribed from ${channelName}`);
  };
}

export function disconnect(): void {
  activeChannels.forEach((ch) => ch.unsubscribe());
  activeChannels.clear();
  connectionListeners.clear();
  client?.close();
  client = null;
}

/**
 * Register a listener for Ably connection state changes.
 * States: 'connected', 'disconnected', 'suspended', 'failed'.
 * Returns an unsubscribe function.
 */
export function onConnectionStateChange(callback: (state: string) => void): () => void {
  // Ensure client is initialized so connection listeners are wired
  getClient();
  connectionListeners.add(callback);
  return () => {
    connectionListeners.delete(callback);
  };
}
