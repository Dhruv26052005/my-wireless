import { TransportType } from './Device';

export interface Route {
  destination: string;
  path: RouteHop[];
  totalCost: number;
  estimatedDeliveryTime: number;
  reliability: number; // 0.0 to 1.0
}

export interface RouteHop {
  deviceId: string;
  transportType: TransportType;
  cost: number; // Based on signal strength, battery, etc.
  estimatedLatency: number;
}

export interface NetworkTopology {
  devices: Map<string, any>; // Map<string, Device> - using any to avoid circular import
  connections: Connection[];
  lastUpdated: number;
}

export interface Connection {
  from: string;
  to: string;
  transportType: TransportType;
  signalStrength: number;
  isActive: boolean;
  lastPing: number;
}

export const createRoute = (data: Partial<Route>): Route => ({
  destination: data.destination || '',
  path: data.path || [],
  totalCost: data.totalCost || 0,
  estimatedDeliveryTime: data.estimatedDeliveryTime || 0,
  reliability: data.reliability || 0.0
});

export const createRouteHop = (data: Partial<RouteHop>): RouteHop => ({
  deviceId: data.deviceId || '',
  transportType: data.transportType || TransportType.BLUETOOTH,
  cost: data.cost || 0,
  estimatedLatency: data.estimatedLatency || 0
});

export const createConnection = (data: Partial<Connection>): Connection => ({
  from: data.from || '',
  to: data.to || '',
  transportType: data.transportType || TransportType.BLUETOOTH,
  signalStrength: data.signalStrength || 0,
  isActive: data.isActive ?? false,
  lastPing: data.lastPing || Date.now()
});
