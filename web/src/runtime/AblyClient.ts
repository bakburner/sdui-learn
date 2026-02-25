import * as Ably from 'ably';

const ABLY_TOKEN_URL = '/ably-token';

let client: Ably.Realtime | null = null;
const activeChannels = new Map<string, Ably.RealtimeChannel>();

function getClient(): Ably.Realtime {
  if (!client) {
    client = new Ably.Realtime({ authUrl: ABLY_TOKEN_URL, autoConnect: true });

    client.connection.on('connected', () => console.log('[Ably] Connected'));
    client.connection.on('disconnected', () => console.warn('[Ably] Disconnected'));
    client.connection.on('failed', (err) => console.error('[Ably] Connection failed', err));
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
  client?.close();
  client = null;
}
