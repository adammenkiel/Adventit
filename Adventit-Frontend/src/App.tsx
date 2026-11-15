import { useEffect, useMemo, useRef, useState } from 'react'
import {
  CalendarDays,
  Clock3,
  Compass,
  Footprints,
  LogOut,
  MapPinned,
  Route,
  ShieldCheck,
  Sparkles,
  TrainFront,
  UserRound,
} from 'lucide-react'
import './App.css'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
const GOOGLE_MAPS_API_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? ''

type AuthMode = 'login' | 'register'
type View = 'dashboard' | 'planner' | 'insights'

type User = { email: string }

type TransitStop = {
  name?: string
}

type TransitLine = {
  name?: string
  vehicleType?: string
}

type TransitDetails = {
  departureStop?: TransitStop
  arrivalStop?: TransitStop
  departureTime?: string
  arrivalTime?: string
  headsign?: string
  transitLine?: TransitLine
}

type NavigationInstruction = {
  maneuver?: string
  instructions?: string
}

type RouteStep = {
  travelMode?: string
  navigationInstruction?: NavigationInstruction
  transitDetails?: TransitDetails
}

type TripRoute = {
  durationSeconds: number
  distanceMeters: number
  encodedPolyline: string
  steps: RouteStep[]
}

type TravelAlternative = {
  travelMode: string
  routes: TripRoute[]
}

type TripPlan = {
  startPlace: string
  endPlace: string
  alternatives: TravelAlternative[]
}

type BannerState = {
  type: 'success' | 'error'
  text: string
} | null

async function requestJson<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers ?? {}),
    },
    ...options,
  })

  const rawText = await response.text()
  const payload = rawText ? JSON.parse(rawText) : null

  if (!response.ok) {
    const message =
      payload?.message ?? payload?.error ?? `Request failed with status ${response.status}`
    throw new Error(message)
  }

  return payload as T
}

function formatDuration(value: number): string {
  if (value <= 0) {
    return '0 min'
  }

  const hours = Math.floor(value / 3600)
  const minutes = Math.floor((value % 3600) / 60)

  if (hours > 0) {
    return `${hours}h ${minutes}m`
  }

  return `${minutes} min`
}

function formatDistance(value: number): string {
  if (value >= 1000) {
    return `${(value / 1000).toFixed(1)} km`
  }

  return `${value} m`
}

function formatMode(travelMode: string): string {
  if (travelMode === 'TRANSIT') {
    return 'Transit'
  }

  if (travelMode === 'WALK') {
    return 'Walking'
  }

  return travelMode
}

type MapPoint = google.maps.LatLngLiteral

type GoogleMapsWindow = typeof window & {
  google?: typeof google
}

let googleMapsPromise: Promise<void> | undefined

function loadGoogleMaps(): Promise<void> {
  const maps = (window as GoogleMapsWindow).google?.maps
  if (maps) {
    return Promise.resolve()
  }

  if (!googleMapsPromise) {
    googleMapsPromise = new Promise<void>((resolve, reject) => {
      const script = document.createElement('script')
      const scriptUrl = new URL('https://maps.googleapis.com/maps/api/js')
      scriptUrl.searchParams.set('key', GOOGLE_MAPS_API_KEY)
      scriptUrl.searchParams.set('v', 'weekly')
      script.src = scriptUrl.toString()
      script.async = true
      script.onload = () => {
        if ((window as GoogleMapsWindow).google?.maps) {
          resolve()
        } else {
          googleMapsPromise = undefined
          reject(new Error('Google Maps JavaScript API did not initialize.'))
        }
      }
      script.onerror = () => {
        googleMapsPromise = undefined
        reject(new Error('Unable to load Google Maps JavaScript API.'))
      }
      document.head.appendChild(script)
    })
  }

  return googleMapsPromise
}

function decodePolyline(encodedPolyline: string): MapPoint[] {
  const points: MapPoint[] = []
  let latitude = 0
  let longitude = 0
  let index = 0

  const decodeCoordinate = () => {
    let value = 0
    let shift = 0

    while (index < encodedPolyline.length && shift <= 30) {
      const chunk = encodedPolyline.charCodeAt(index++) - 63
      if (chunk < 0 || chunk > 63) {
        return null
      }

      value += (chunk & 0x1f) * 2 ** shift
      if (chunk < 0x20) {
        return value % 2 === 1 ? -(value + 1) / 2 : value / 2
      }
      shift += 5
    }

    return null
  }

  while (index < encodedPolyline.length) {
    const latitudeDelta = decodeCoordinate()
    const longitudeDelta = decodeCoordinate()
    if (latitudeDelta === null || longitudeDelta === null) {
      return []
    }

    latitude += latitudeDelta
    longitude += longitudeDelta
    const point = { lat: latitude / 1e5, lng: longitude / 1e5 }
    if (point.lat < -90 || point.lat > 90 || point.lng < -180 || point.lng > 180) {
      return []
    }
    points.push(point)
  }

  return points
}

function RouteMap({
  encodedPolyline,
  startPlace,
  endPlace,
}: {
  encodedPolyline: string
  startPlace: string
  endPlace: string
}) {
  const mapElement = useRef<HTMLDivElement>(null)
  const [mapError, setMapError] = useState('')
  const points = useMemo(() => decodePolyline(encodedPolyline), [encodedPolyline])

  useEffect(() => {
    if (!GOOGLE_MAPS_API_KEY || points.length < 2 || !mapElement.current) {
      return
    }

    let cancelled = false
    let routeLine: google.maps.Polyline | undefined

    void loadGoogleMaps()
      .then(() => {
        if (cancelled || !mapElement.current) {
          return
        }

        const maps = (window as GoogleMapsWindow).google?.maps
        if (!maps) {
          throw new Error('Google Maps JavaScript API did not initialize.')
        }

        const map = new maps.Map(mapElement.current, {
          center: points[0],
          zoom: 12,
          mapTypeControl: false,
          streetViewControl: false,
          fullscreenControl: true,
        })
        const bounds = new maps.LatLngBounds()
        points.forEach((point) => bounds.extend(point))
        map.fitBounds(bounds, 48)
        routeLine = new maps.Polyline({
          path: points,
          geodesic: true,
          strokeColor: '#06b6d4',
          strokeOpacity: 0.95,
          strokeWeight: 6,
        })
        routeLine.setMap(map)
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setMapError(
            error instanceof Error ? error.message : 'Unable to display this route on Google Maps.',
          )
        }
      })

    return () => {
      cancelled = true
      routeLine?.setMap(null)
    }
  }, [encodedPolyline, points])

  return (
    <div className="route-map-wrap">
      <div className="route-map-heading">
        <strong>Route map</strong>
        <span title={`${startPlace} → ${endPlace}`}>{startPlace} → {endPlace}</span>
      </div>
      {GOOGLE_MAPS_API_KEY && points.length >= 2 ? (
        <>
          <div
            className="route-map"
            ref={mapElement}
            role="region"
            aria-label={`Google Map showing the route from ${startPlace} to ${endPlace}`}
          />
          {mapError ? <div className="route-map-unavailable" role="alert">{mapError}</div> : null}
        </>
      ) : (
        <div className="route-map-unavailable" role="status">
          {!GOOGLE_MAPS_API_KEY
            ? 'Set VITE_GOOGLE_MAPS_API_KEY to display the route returned by the trip planner.'
            : 'The trip planner did not return a valid route path.'}
        </div>
      )}
    </div>
  )
}

function App() {
  const [view, setView] = useState<View>('dashboard')
  const [authMode, setAuthMode] = useState<AuthMode>('login')
  const [user, setUser] = useState<User | null>(null)
  const [authPending, setAuthPending] = useState(false)
  const [banner, setBanner] = useState<BannerState>(null)
  const [loadingUser, setLoadingUser] = useState(true)
  const [authForm, setAuthForm] = useState({ email: '', password: '' })
  const [tripForm, setTripForm] = useState({
    departureDate: '',
    departureTime: '',
    startPlace: '',
    endPlace: '',
  })
  const [tripResult, setTripResult] = useState<TripPlan | null>(null)
  const [tripPending, setTripPending] = useState(false)

  const fetchCurrentUser = async () => {
    try {
      const currentUser = await requestJson<User>('/api/auth/me')
      setUser(currentUser)
      setBanner(null)
    } catch {
      setUser(null)
    } finally {
      setLoadingUser(false)
    }
  }

  useEffect(() => {
    void fetchCurrentUser()
  }, [])

  const handleAuthInput = (field: 'email' | 'password', value: string) => {
    setAuthForm((current) => ({ ...current, [field]: value }))
  }

  const handlePlannerInput = (
    field: 'departureDate' | 'departureTime' | 'startPlace' | 'endPlace',
    value: string,
  ) => {
    setTripForm((current) => ({ ...current, [field]: value }))
  }

  const handleAuthSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const email = authForm.email.trim()
    const password = authForm.password

    if (!email || !password) {
      setBanner({ type: 'error', text: 'Email and password are required.' })
      return
    }

    setAuthPending(true)
    setBanner(null)

    try {
      const endpoint = authMode === 'login' ? '/api/auth/login' : '/api/auth/register'
      await requestJson<{ email: string }>(endpoint, {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })

      if (authMode === 'register') {
        await requestJson<{ email: string }>('/api/auth/login', {
          method: 'POST',
          body: JSON.stringify({ email, password }),
        })
      }

      await fetchCurrentUser()
      setAuthForm({ email: '', password: '' })
      setBanner({
        type: 'success',
        text:
          authMode === 'register'
            ? 'Account created successfully. You are now signed in.'
            : 'Welcome back. Your trip dashboard is ready.',
      })
    } catch (error) {
      setBanner({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to complete authentication.',
      })
    } finally {
      setAuthPending(false)
    }
  }

  const handleLogout = async () => {
    try {
      await requestJson('/api/auth/logout', {
        method: 'POST',
      })
      setUser(null)
      setView('dashboard')
      setBanner({ type: 'success', text: 'You have been signed out.' })
    } catch (error) {
      setBanner({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to sign out.',
      })
    }
  }

  const handlePlanTrip = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const startPlace = tripForm.startPlace.trim()
    const endPlace = tripForm.endPlace.trim()

    if (!startPlace || !endPlace) {
      setBanner({ type: 'error', text: 'Departure and destination are required.' })
      return
    }

    setTripPending(true)
    setBanner(null)

    try {
      const result = await requestJson<TripPlan>('/app/trip', {
        method: 'POST',
        body: JSON.stringify({
          startPlace,
          endPlace,
          departureDate: tripForm.departureDate,
          departureTime: tripForm.departureTime,
        }),
      })

      setTripResult(result)
      setView('insights')
      setBanner({ type: 'success', text: 'Route suggestions have been generated.' })
    } catch (error) {
      setBanner({
        type: 'error',
        text: error instanceof Error ? error.message : 'Unable to plan this route.',
      })
    } finally {
      setTripPending(false)
    }
  }

  const travelModes = tripResult?.alternatives ?? []

  if (loadingUser) {
    return (
      <div className="app-shell">
        <div className="panel auth-panel loading-panel">
          <div className="spinner" aria-label="Loading account" />
          <p>Loading your travel dashboard...</p>
        </div>
      </div>
    )
  }

  if (!user) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <div className="auth-copy">
            <span className="eyebrow">Adventit</span>
            <h1>Plan every journey with clarity.</h1>
            <p>
              Build route options, compare travel modes, and travel confidently between any two
              locations.
            </p>
            <ul className="feature-list">
              <li>
                <ShieldCheck size={18} /> Secure sign-in with cookie-based authentication
              </li>
              <li>
                <Route size={18} /> Live route planning for walking and transit journeys
              </li>
              <li>
                <Sparkles size={18} /> Modern trip insights that feel like a premium planner
              </li>
            </ul>
          </div>

          <form className="panel auth-form" onSubmit={handleAuthSubmit}>
            <div className="auth-header">
              <h2>{authMode === 'login' ? 'Welcome back' : 'Create your account'}</h2>
              <div className="toggle-row">
                <button
                  type="button"
                  className={authMode === 'login' ? 'tab tab-active' : 'tab'}
                  onClick={() => setAuthMode('login')}
                >
                  Login
                </button>
                <button
                  type="button"
                  className={authMode === 'register' ? 'tab tab-active' : 'tab'}
                  onClick={() => setAuthMode('register')}
                >
                  Register
                </button>
              </div>
            </div>

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={authForm.email}
                onChange={(event) => handleAuthInput('email', event.target.value)}
                placeholder="traveler@email.com"
              />
            </label>

            <label className="field">
              <span>Password</span>
              <input
                type="password"
                value={authForm.password}
                onChange={(event) => handleAuthInput('password', event.target.value)}
                placeholder="••••••••"
              />
            </label>

            {banner ? <div className={`banner ${banner.type}`}>{banner.text}</div> : null}

            <button className="primary-button" type="submit" disabled={authPending}>
              {authPending
                ? authMode === 'login'
                  ? 'Signing in...'
                  : 'Creating account...'
                : authMode === 'login'
                  ? 'Sign in'
                  : 'Create account'}
            </button>
          </form>
        </div>
      </div>
    )
  }

  return (
    <div className="app-shell">
      <header className="topbar panel">
        <div className="brand-wrap">
          <div className="brand-mark">
            <Compass size={18} />
          </div>
          <div>
            <p className="brand-name">Adventit</p>
            <p className="brand-subtitle">Travel planner</p>
          </div>
        </div>

        <nav className="nav-group" aria-label="Main navigation">
          <button
            type="button"
            className={view === 'dashboard' ? 'nav-item active' : 'nav-item'}
            onClick={() => setView('dashboard')}
          >
            Dashboard
          </button>
          <button
            type="button"
            className={view === 'planner' ? 'nav-item active' : 'nav-item'}
            onClick={() => setView('planner')}
          >
            Planner
          </button>
          <button
            type="button"
            className={view === 'insights' ? 'nav-item active' : 'nav-item'}
            onClick={() => setView('insights')}
          >
            Insights
          </button>
        </nav>

        <div className="user-wrap">
          <div className="user-badge">
            <UserRound size={16} />
            <span>{user.email}</span>
          </div>
          <button type="button" className="secondary-button" onClick={handleLogout}>
            <LogOut size={16} />
            Sign out
          </button>
        </div>
      </header>

      {banner ? <div className={`banner ${banner.type}`}>{banner.text}</div> : null}

      <main className="content-shell">
        {view === 'dashboard' ? (
          <>
            <section className="hero-grid">
              <div className="panel hero-panel">
                <span className="eyebrow">Smart travel</span>
                <h1>Keep every route flexible and on schedule.</h1>
                <p>
                  Explore the quickest city routes, compare transit and walking options, and plan
                  the next leg of your journey with clarity.
                </p>
                <div className="hero-actions">
                  <button type="button" className="primary-button" onClick={() => setView('planner')}>
                    Plan a trip
                  </button>
                  <button type="button" className="ghost-button" onClick={() => setView('insights')}>
                    View insights
                  </button>
                </div>
              </div>

              <div className="panel summary-panel">
                <div className="summary-header">
                  <span>Trip overview</span>
                  <Sparkles size={18} />
                </div>
                <div className="metric-grid">
                  <div className="metric-card accent">
                    <PlaneTakeoffIcon />
                    <strong>{tripResult ? tripResult.alternatives.length : 2}</strong>
                    <span>available modes</span>
                  </div>
                  <div className="metric-card">
                    <MapPinned size={18} />
                    <strong>{tripResult ? tripResult.startPlace : 'Ready'}</strong>
                    <span>current origin</span>
                  </div>
                  <div className="metric-card">
                    <Clock3 size={18} />
                    <strong>{tripResult ? formatDuration(tripResult.alternatives[0]?.routes[0]?.durationSeconds ?? 0) : '—'}</strong>
                    <span>latest trip time</span>
                  </div>
                </div>
              </div>
            </section>

            <section className="feature-grid">
              <div className="panel info-card">
                <div className="icon-wrap teal">
                  <TrainFront size={18} />
                </div>
                <h3>Transit routes</h3>
                <p>Compare public transport itineraries with departure and arrival details.</p>
              </div>
              <div className="panel info-card">
                <div className="icon-wrap amber">
                  <Footprints size={18} />
                </div>
                <h3>Walking options</h3>
                <p>See walking and short-distance travel alternatives for your route.</p>
              </div>
              <div className="panel info-card">
                <div className="icon-wrap blue">
                  <CalendarDays size={18} />
                </div>
                <h3>Trip planning</h3>
                <p>Capture key trip details, then review route suggestions and travel steps.</p>
              </div>
            </section>
          </>
        ) : null}

        {view === 'planner' ? (
          <section className="panel planner-panel">
            <div className="section-heading">
              <div>
                <span className="eyebrow">Travel planner</span>
                <h2>Build your next route</h2>
              </div>
            </div>

            <form className="planner-form" onSubmit={handlePlanTrip}>
              <div className="field-row two-up">
                <label className="field">
                  <span>Departure date</span>
                  <input
                    type="date"
                    max="9999-12-31"
                    value={tripForm.departureDate}
                    onChange={(event) => handlePlannerInput('departureDate', event.target.value)}
                  />
                </label>

                <label className="field">
                  <span>Departure time</span>
                  <input
                    type="time"
                    value={tripForm.departureTime}
                    onChange={(event) => handlePlannerInput('departureTime', event.target.value)}
                  />
                </label>
              </div>

              <div className="field-row two-up">
                <label className="field">
                  <span>Departure location</span>
                  <input
                    type="text"
                    value={tripForm.startPlace}
                    onChange={(event) => handlePlannerInput('startPlace', event.target.value)}
                    placeholder="Central Station, Berlin"
                  />
                </label>

                <label className="field">
                  <span>Destination</span>
                  <input
                    type="text"
                    value={tripForm.endPlace}
                    onChange={(event) => handlePlannerInput('endPlace', event.target.value)}
                    placeholder="City Museum, Berlin"
                  />
                </label>
              </div>

              <div className="form-actions">
                <button type="submit" className="primary-button" disabled={tripPending}>
                  {tripPending ? 'Planning route...' : 'Plan route'}
                </button>
              </div>
            </form>
          </section>
        ) : null}

        {view === 'insights' ? (
          <section className="results-panel">
            {travelModes.length === 0 ? (
              <div className="panel empty-state">
                <h3>No route planned yet</h3>
                <p>Use the planner to compare every leg of your journey.</p>
              </div>
            ) : (
              travelModes.map((alternative) => (
                <div className="panel route-panel" key={alternative.travelMode}>
                  <div className="route-header">
                    <div className="route-title-wrap">
                      <div className="icon-wrap teal">
                        {alternative.travelMode === 'TRANSIT' ? <TrainFront size={18} /> : <Footprints size={18} />}
                      </div>
                      <div>
                        <span className="eyebrow">Mode</span>
                        <h3>{formatMode(alternative.travelMode)}</h3>
                      </div>
                    </div>
                    <span className="pill">
                      {alternative.routes.length} route{alternative.routes.length === 1 ? '' : 's'}
                    </span>
                  </div>

                  {alternative.routes.map((route, routeIndex) => (
                    <div className="route-card" key={`${alternative.travelMode}-${routeIndex}`}>
                      <div className="route-stats">
                        <div>
                          <span>Duration</span>
                          <strong>{formatDuration(route.durationSeconds)}</strong>
                        </div>
                        <div>
                          <span>Distance</span>
                          <strong>{formatDistance(route.distanceMeters)}</strong>
                        </div>
                        <div>
                          <span>Route</span>
                          <strong>{routeIndex + 1}</strong>
                        </div>
                      </div>

                      <RouteMap
                        encodedPolyline={route.encodedPolyline}
                        startPlace={tripResult?.startPlace ?? 'Origin'}
                        endPlace={tripResult?.endPlace ?? 'Destination'}
                      />

                      <div className="steps-list">
                        {route.steps.length === 0 ? (
                          <p>No detailed steps available for this route.</p>
                        ) : (
                          route.steps.map((step, index) => (
                            <div className="step-item" key={`${routeIndex}-${index}`}>
                              <div className="step-marker">{index + 1}</div>
                              <div className="step-copy">
                                <div className="step-header">
                                  <span className="step-mode">{step.travelMode ?? formatMode(alternative.travelMode)}</span>
                                  <span className="step-time">
                                    {step.transitDetails?.departureTime
                                      ? new Date(step.transitDetails.departureTime).toLocaleTimeString([], {
                                          hour: 'numeric',
                                          minute: '2-digit',
                                        })
                                      : '—'}
                                  </span>
                                </div>

                                <p>
                                  {step.navigationInstruction?.instructions ??
                                    step.transitDetails?.headsign ??
                                    `Travel from ${tripResult?.startPlace ?? 'origin'} to ${tripResult?.endPlace ?? 'destination'}`}
                                </p>

                                {step.transitDetails ? (
                                  <div className="transit-meta">
                                    <span>
                                      {step.transitDetails.departureStop?.name ?? 'Departure'} →{' '}
                                      {step.transitDetails.arrivalStop?.name ?? 'Arrival'}
                                    </span>
                                    <span>
                                      {step.transitDetails.transitLine?.name ?? 'Transit'} •{' '}
                                      {step.transitDetails.transitLine?.vehicleType ?? 'service'}
                                    </span>
                                  </div>
                                ) : null}
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              ))
            )}
          </section>
        ) : null}
      </main>
    </div>
  )
}

function PlaneTakeoffIcon() {
  return <Route className="metric-icon" size={18} />
}

export default App
