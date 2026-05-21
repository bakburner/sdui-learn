import React, { createContext, useContext } from 'react';

const WireAssetBaseUrlContext = createContext('');

export function WireAssetBaseUrlProvider({
  baseUrl,
  children,
}: {
  baseUrl?: string;
  children: React.ReactNode;
}): React.ReactElement {
  return (
    <WireAssetBaseUrlContext.Provider value={baseUrl ?? ''}>
      {children}
    </WireAssetBaseUrlContext.Provider>
  );
}

export function useWireAssetBaseUrl(): string {
  return useContext(WireAssetBaseUrlContext);
}
