# Architecture and Design Decisions

This document records significant architectural and design decisions made while
refactoring and extending the original university project into the current Drone
Photo Service application.

The records focus on decisions that materially affected application structure,
responsibility boundaries, simulation behavior, reliability, or maintainability.
They document the context in which a problem arose, the decision that was made,
relevant alternatives, and the consequences of the chosen approach. They are not
intended to record every implementation choice or to provide a chronological
development log.

Each record primarily answers why the current design was chosen. The resulting
architecture and implementation are described in greater detail in the
Architecture document, while the broader sequence of project changes is
summarized in the Project Evolution document.

## Contents

1. [ADR 001: MVC Separation](#adr-001-mvc-separation)
2. [ADR 002: Controller Responsibilities](#adr-002-controller-responsibilities)
3. [ADR 003: Simulation Time](#adr-003-simulation-time)
4. [ADR 004: Snapshots for View State](#adr-004-snapshots-for-view-state)
5. [ADR 005: Drone State Transitions](#adr-005-drone-state-transitions)
6. [ADR 006: Validation Boundaries](#adr-006-validation-boundaries)
7. [ADR 007: Error Propagation](#adr-007-error-propagation)
8. [ADR 008: Event Log](#adr-008-event-log)
9. [ADR 009: Thread and Lifecycle Management](#adr-009-thread-and-lifecycle-management)
10. [ADR 010: Testing Scope](#adr-010-testing-scope)
11. [ADR 011: Map Metadata and Scale](#adr-011-map-metadata-and-scale)

[//]: # (Each record should answer the question Why?)

<hr>

## ADR 001: MVC Separation

### Status

Accepted.

### Context

The project originated as a university assignment organized around a
conventional Model-View-Controller (MVC) architecture consisting of one `Model`,
one `View`, and one `Controller`. The application entry point created the
controller, which in turn instantiated and owned the shared model and view. As
additional functionality was introduced, the responsibilities of these three
principal classes expanded considerably. The controller gradually became
responsible for application initialization, command handling, simulation
lifecycle management, GUI refresh, map loading, playback, image export, dialog
coordination, and other application features. The model similarly accumulated
responsibilities beyond simulation state, including image processing, actor
management, thread-pool management, task archival, map handling, and file
export.

### Problem

The original MVC architecture remained functional throughout the project's
development. However, as the application grew, the responsibilities of the
central MVC classes became increasingly broad. The controller combined
application composition with numerous unrelated operational concerns, while the
model gradually accumulated infrastructure responsibilities beyond its primary
role as the owner of simulation state. Although the architecture remained
understandable, ownership of responsibilities and subsystem boundaries became
less explicit.

### Decision

The existing MVC architecture was retained as the primary architectural
structure. Rather than replacing it with another architectural pattern, the
project was refactored incrementally to establish clearer architectural
boundaries.

Application construction was moved to the application entry point, where the
shared model and view are created explicitly and injected into the root
controller. The root controller was redefined primarily as the application's
composition and coordination component, while feature-specific behavior was
delegated to specialized controllers. Responsibilities that no longer naturally
belonged in the central model were progressively extracted into dedicated model
and support classes.

Further refinements to the model-view boundary are documented in `ADR 004`,
which introduces snapshots and presentation-specific view data as the interface
between the simulation domain and graphical presentation.

### Alternatives Considered

Replacing the MVC architecture with a different architectural pattern was
considered unnecessary because the existing structure already matched the
application's interaction model. Retaining the original architecture while
continuing to expand the central controller and model was also rejected because
it would further increase coupling and reduce the clarity of responsibility
ownership. The chosen approach therefore emphasized incremental architectural
refactoring rather than architectural replacement.

### Consequences

The refactoring produced clearer ownership of application behavior, reduced the
responsibilities of the root controller, and established more explicit
boundaries between simulation logic, presentation, and application coordination.
It also provided the architectural foundation for subsequent improvements,
including controller specialization, simulation-time management, validation,
lifecycle management, event logging, and presentation snapshots.

The primary cost of this decision is an increased number of collaborating
classes together with more explicit dependency wiring. This additional
structural complexity is considered acceptable because it improves
maintainability, readability, and long-term extensibility.

### Current Implementation

The application is organized around one shared `Model`, one shared `View`, and
one root `Controller` responsible for composing the controller layer.
Controllers provide the communication path between the model and the view, while
feature-specific behavior is implemented by dedicated controller classes.
Additional architectural refinements are described in the corresponding
architecture documents and related decision records.

### Related Documentation

See the `MVC Architecture` document for the current architectural organization,
`ADR 002` for the decomposition of the controller layer, and `ADR 004` for the
introduction of snapshots and presentation-specific view data.

<hr>

## ADR 002: Controller Responsibilities

### Status

Accepted.

### Context

The original architecture employed a single controller that coordinated nearly
all application behavior. Besides acting as the application's central
coordination point, it interpreted user commands, controlled the simulation
lifecycle, refreshed the graphical user interface, managed map loading,
synchronized model and view state, controlled task playback, exported images,
displayed dialogs, and coordinated many other application features. As the
project evolved, the controller became the primary location for introducing new
functionality.

Although these responsibilities were related to application control, they
represented several independent concerns with different lifecycles,
dependencies, and maintenance requirements.

### Problem

The controller gradually became responsible for an increasingly diverse
collection of application features. Individual methods remained understandable,
but the controller itself evolved into a central implementation point for much
of the application's behavior. This made it more difficult to identify ownership
of individual responsibilities, increased coupling between otherwise unrelated
features, and reduced the independence with which different parts of the
application could evolve.

### Decision

The controller layer was reorganized by separating feature-specific behavior
into dedicated controllers while retaining a single root controller responsible
for application composition.

The root controller constructs the controller graph, establishes dependencies
between controllers, registers listeners with the view, and coordinates
application startup and shutdown. Operational responsibilities are delegated to
specialized controllers responsible for areas such as simulation lifecycle
management, view refresh, selection management, command dispatch, map loading,
playback, archive viewing, image export, and user interface state.

The controller layer continues to provide the only communication path between
the model and the view, but responsibility for implementing application behavior
is distributed according to functional concerns rather than being concentrated
in a single controller class.

### Alternatives Considered

Maintaining a single controller was rejected because continued growth would
further increase coupling between unrelated application features and reduce the
clarity of responsibility ownership.

Creating a large number of highly granular controllers was also rejected because
it would unnecessarily fragment application behavior and complicate
coordination. Instead, responsibilities were grouped into controllers
representing coherent application services with well-defined areas of
responsibility.

### Consequences

The controller layer now provides clearer ownership of application behavior and
improves separation between independent application concerns. Individual
controllers can evolve with minimal impact on unrelated functionality, and
responsibilities such as simulation lifecycle management, selection handling,
map loading, and view refresh have explicit implementation boundaries.

The primary cost of this decision is additional coordination between controllers
together with a greater number of collaborating classes. This complexity is
managed by keeping dependency wiring within the root controller, allowing
specialized controllers to remain focused on their respective responsibilities.

### Current Implementation

The root `Controller` serves as the application's composition and coordination
point. Specialized controllers implement simulation lifecycle management,
command dispatch, view refresh, control-state management, selection handling,
map loading, playback, archive viewing, and image export. Communication between
controllers is performed through explicit dependencies and narrowly scoped
callbacks rather than through shared implementation logic.

### Related Documentation

See the `Controllers` architecture document for the current organization of the
controller layer and `ADR 001` for the architectural rationale behind the
overall MVC refinement.

<hr>

## ADR 003: Simulation Time

### Status

Accepted.

### Context

The original implementation relied on wall-clock time for several aspects of the
simulation. Application behavior, timestamps, and elapsed durations were derived
from the execution speed of the host system. While this approach was sufficient
for a simple real-time simulation, it coupled simulation progress to the
operating environment and made application behavior dependent on processor
performance, scheduling, pauses, and execution speed.

As the project evolved, additional functionality such as simulation speed
control, deterministic testing, event logging, task history, and playback
required a notion of time that represented the simulated world rather than the
execution environment.

### Problem

Wall-clock time does not represent simulation time. Operations such as pausing
the simulation, changing the simulation speed, or executing the application on
systems with different performance characteristics would produce timestamps and
elapsed durations that no longer reflected the simulated behavior.

The use of wall-clock time also reduced determinism, complicated testing, and
made different parts of the application rely on different notions of elapsed
time.

### Decision

The application adopts simulation time as the authoritative representation of
time throughout the simulation.

Simulation time advances only as part of the simulation update cycle and is
independent of wall-clock execution speed. Components requiring temporal
information obtain it from the shared simulation clock rather than directly from
the operating system.

Wall-clock time remains appropriate only where interaction with the external
environment requires real calendar time, such as generating timestamps for
exported files.

### Alternatives Considered

Continuing to rely on wall-clock time was rejected because it prevented
simulation speed from accurately representing the simulated world and reduced
the determinism of the application.

Maintaining both simulation time and wall-clock time throughout the simulation
was also rejected because it would introduce multiple competing notions of
elapsed time and increase the risk of inconsistent behavior. Instead, simulation
time became the single authoritative source of temporal information within the
simulation domain.

### Consequences

Simulation behavior is independent of processor performance and execution speed.
Pausing the simulation freezes simulated time, while changing the simulation
speed affects the rate at which simulated time advances without changing the
logical behavior of the simulation.

Using a single simulation clock improves consistency between simulation state,
event history, battery calculations, task timestamps, and other time-dependent
subsystems. It also simplifies deterministic testing because temporal behavior
can be reproduced independently of wall-clock execution.

The principal cost of this decision is that components interacting with
real-world resources must explicitly distinguish between simulation time and
wall-clock time.

### Current Implementation

The model owns a shared simulation clock that advances as part of the simulation
update cycle. Controllers and model components obtain temporal information from
this shared simulation time rather than directly from system time. The remaining
use of wall-clock time is limited to interactions with the external environment,
such as timestamping exported files.

### Related Documentation

See the `Simulation Time` architecture document for the implementation of the
simulation clock, `ADR 008` for the use of simulation time in the event log, and
`ADR 010` for its impact on deterministic testing.

<hr>

## ADR 004: Snapshots for View State

### Status

Accepted.

### Context

The original presentation layer accessed domain objects directly when rendering
application state. User interface components therefore depended on mutable
simulation objects whose primary purpose was to represent the simulation domain
rather than graphical presentation.

As the application evolved, the controller layer became responsible for
coordinating background simulation, graphical refresh, selection state, and
increasingly rich presentation features. The direct use of domain objects by the
view made the separation between simulation logic and presentation progressively
less explicit.

### Problem

Simulation objects and presentation objects have different responsibilities.
Domain objects represent the current simulation state and continue to evolve
while the simulation is running, whereas graphical presentation requires a
stable, read-only representation of that state during rendering.

Passing mutable domain objects directly to the presentation layer increased
coupling between the model and the view, exposed implementation details outside
the model, and complicated the coordination of background simulation with
graphical updates.

### Decision

The model-view boundary was redefined to exchange presentation-specific
snapshots and view data instead of mutable simulation objects.

Snapshots provide read-only representations of simulation state intended solely
for graphical presentation. Controllers retrieve snapshots from the model,
transform them into presentation-specific view data where appropriate, and pass
those objects to the view for rendering.

The simulation domain therefore remains responsible for maintaining application
state, while the presentation layer operates exclusively on read-only
representations of that state.

### Alternatives Considered

Continuing to expose mutable domain objects directly to the presentation layer
was rejected because it unnecessarily coupled graphical rendering to simulation
implementation and made presentation dependent on mutable application state.

Performing presentation-specific transformations directly inside the view was
also rejected because it would move presentation logic into the graphical user
interface and weaken the separation between the controller and view layers.

The adopted approach therefore establishes read-only snapshots as the interface
between the simulation domain and graphical presentation.

### Consequences

The model and presentation layers are more clearly separated, and graphical
rendering no longer depends directly on mutable simulation objects. Presentation
components operate on stable, read-only data, while simulation objects remain
internal to the model.

The controller layer becomes responsible for translating simulation state into
presentation state, providing a natural location for view-specific
transformations without introducing presentation concerns into the model.

The principal cost of this decision is the introduction of additional snapshot
and view-data classes together with the corresponding transformation logic. This
additional structure is considered acceptable because it improves architectural
separation, reduces coupling, and simplifies future evolution of both the
simulation domain and the graphical presentation.

### Current Implementation

Controllers obtain read-only snapshots and presentation-specific view data from
the model before updating the graphical user interface. Swing components render
only these presentation objects and do not access mutable simulation objects
directly. Snapshot creation remains the responsibility of the model, while
presentation-specific transformations are coordinated by the controller layer.
Drone snapshots copy component values into immutable battery, camera, and motor
snapshots. Some image-bearing snapshots still share image references to avoid
expensive deep copies, so the presentation treats those images as read-only;
strict deep immutability is not currently guaranteed for every snapshot type.

### Related Documentation

See the `Snapshots and View Data` architecture document for the organization of
presentation objects, the `Application Architecture` document for the overall
model-view boundary, and `ADR 001` for the architectural refinement of the MVC
structure.

<hr>

## ADR 005: Drone State Transitions

### Status

Accepted.

### Context

A drone progresses through several operational states during the simulation,
including waiting for work, traveling to a task, processing a task, returning to
base, and charging. State changes affect drone behavior and are also relevant to
presentation, monitoring, and event history.

As the drone lifecycle became more detailed, state changes occurred at several
points in its behavior. Treating state as an ordinary mutable property made the
transition itself implicit and provided no single location for behavior that
should accompany every state change.

### Problem

A drone state represents a lifecycle transition rather than an arbitrary mutable
value. Allowing state to be assigned directly makes it possible to change the
lifecycle without consistently applying transition-related behavior and
distributes responsibility for state management across the drone implementation.

This becomes increasingly problematic when state changes are observed outside
the immediate movement logic, for example by presentation snapshots, monitoring,
or event logging.

### Decision

Drone state changes are performed through an explicit state-transition operation
rather than through a general-purpose state setter.

The drone remains responsible for determining when a transition is required as
part of its domain behavior. The transition operation centralizes modification
of the current state and provides a single boundary for behavior associated with
entering a new state.

The state machine remains implemented within the `Drone` domain object rather
than being extracted into a separate state-machine framework or hierarchy of
state classes.

### Alternatives Considered

Retaining a general-purpose state setter was rejected because it treats
lifecycle state as unrestricted mutable data and provides no explicit semantic
distinction between assigning a value and performing a state transition.

Extracting each drone state into a separate implementation of the State design
pattern was also considered unnecessary. The number of states and transition
rules does not justify the additional classes and indirection, and the
transition behavior remains closely related to the drone's existing domain
logic.

### Consequences

State changes have an explicit semantic boundary and are controlled by the drone
rather than by external components. Transition-related behavior can be applied
consistently from one location, while presentation and monitoring components can
treat the resulting state as read-only domain information.

The approach preserves a comparatively simple drone implementation while making
its lifecycle more explicit. The principal cost is that transition rules remain
distributed among the drone operations that determine when transitions occur
rather than being represented as a separate declarative state-transition model.

### Current Implementation

`Drone` maintains its current `DroneState` internally and changes it through a
dedicated transition operation. Domain behavior determines when transitions
occur as the drone accepts work, travels, processes tasks, returns to base, and
charges. External components observe drone state through model data and
presentation snapshots rather than controlling transitions directly. The state
value describes the current lifecycle phase, while the conditions that cause
transitions remain part of the drone operations responsible for movement,
charging, task processing, and return behavior.

### Related Documentation

See the `Drone State Machine` architecture document for the current states,
transition flow, and state-dependent behavior. The presentation of drone state
through read-only view state is described in `ADR 004`.

<hr>

## ADR 006: Validation boundaries

### Status

Accepted.

### Context

The application accepts input and configuration from several sources, including
user interface controls, map metadata, image files, and application settings.
Invalid values can therefore enter the application at different boundaries and
may affect both presentation and simulation behavior.

As validation was strengthened, the same value could potentially be checked by
the view, controller, and model. A consistent rule was required to determine
which layer is responsible for rejecting invalid input and which validations
represent model invariants rather than user-interface constraints.

### Problem

Validation performed only in the graphical user interface does not protect the
model when operations are invoked from another caller or directly from tests.
Conversely, moving all validation into the model would require invalid input to
travel unnecessarily through the application and would prevent the user
interface from providing immediate feedback for simple input constraints.

Duplicating identical validation indiscriminately across layers would also make
validation rules harder to maintain and could produce inconsistent behavior.

### Decision

Validation is performed at the boundary that owns the corresponding constraint.

The view performs lightweight input validation when a constraint belongs to the
presentation or input format. Controllers validate interaction-specific
preconditions and coordinate failures that cross the model–view boundary. The
model validates domain invariants and rejects values that would place
application state into an invalid configuration.

Validation of external resources is performed when those resources enter the
application. Parsed map metadata, image dimensions, scale values, and other
externally supplied data must satisfy the model requirements before they are
committed to application state.

Validation may therefore occur at more than one layer when the layers protect
different concerns. Presentation validation improves immediate user feedback,
while model validation remains authoritative for domain correctness.

### Alternatives Considered

Relying exclusively on user-interface validation was rejected because the model
must remain valid independently of the graphical interface and must protect its
invariants when invoked directly.

Relying exclusively on model validation was also rejected because simple
presentation constraints can be detected earlier and communicated more directly
to the user.

Duplicating every validation rule across all layers was rejected because it
would obscure ownership and increase maintenance requirements. Validation is
instead repeated only when separate architectural boundaries require independent
protection.

### Consequences

Invalid input is rejected close to the boundary where its constraint is defined,
while model invariants remain protected independently of the graphical
interface. This allows controllers and tests to interact with the model without
relying on prior user-interface validation.

Some values may be checked more than once when presentation requirements and
model invariants overlap. This duplication is intentional when the checks serve
different purposes: early feedback at the input boundary and authoritative
protection at the domain boundary.

The separation also distinguishes validation from error presentation. Validation
determines whether input or state is acceptable, while propagation and
presentation of resulting failures are handled according to the application's
error-handling policy.

### Current Implementation

User-facing input is checked before application operations are performed where
immediate validation is appropriate. Controllers mediate validation failures
that occur during user-triggered operations, while model methods reject values
that violate domain invariants. External map data is validated before it
replaces the active map state, preventing partially valid resources from leaving
the model in an inconsistent configuration.

### Related Documentation

See the `Validation and Error Handling` architecture document for the current
validation flow and `ADR 007` for exception propagation and user-facing error
reporting. Map-specific validation and scale constraints are documented in
`ADR 011`.

## ADR 007: Error propagation

### Status

Accepted.

### Context

Application operations may fail at several levels, including file access, image
decoding and processing, metadata parsing, validation, background execution, and
user-triggered workflows. Earlier implementations handled some failures close to
where they occurred, including direct stack-trace output or presentation calls
from code that did not own user interaction.

As error handling was reviewed, a consistent distinction was required between
detecting a failure, propagating technical information, recording diagnostic
information, and presenting an understandable result to the user.

### Problem

Handling exceptions immediately at their point of origin can prevent higher
architectural layers from determining the appropriate application response.
Low-level components generally lack sufficient context to decide whether an
operation should be retried, abandoned, logged, or reported to the user.

Allowing implementation-specific exceptions to propagate unchanged through
multiple layers also exposes internal details and requires controllers to
understand failures belonging to lower-level subsystems. Conversely, suppressing
exceptions or relying on direct stack-trace output makes failures difficult to
handle consistently and provides no controlled application response.

### Decision

Failures propagate outward until they reach the architectural layer that has
sufficient context to handle them meaningfully.

Low-level components throw or propagate exceptions rather than presenting errors
directly. Where several implementation-specific failures represent one
application-level operation, they are translated into a dedicated exception that
describes that operation while retaining the original exception as its cause.

Controllers form the principal error-handling boundary for user-triggered
operations. They catch expected application-level failures, record technical
diagnostic information where appropriate, and request a concise user-facing
error message from the view. The view is responsible only for presenting the
supplied message and does not interpret technical exceptions.

Exceptions are not suppressed solely to keep an operation running. A failure is
either handled at a boundary that can provide a defined recovery or presentation
response, or it is allowed to propagate to a higher-level boundary.

### Alternatives Considered

Handling every exception at its point of origin was rejected because low-level
components generally do not have enough application context to determine the
appropriate response and should not depend on presentation behavior.

Propagating all implementation-specific exceptions directly to controllers was
rejected because it would couple application coordination to details of file
access, parsing, image processing, and other lower-level operations.

Printing stack traces directly was rejected as an error-handling strategy
because it neither defines application behavior nor provides structured
diagnostic reporting. Technical failures that are intentionally handled are
instead recorded through the application's logging mechanism.

### Consequences

Error handling follows the application's architectural boundaries. Model and
support components remain independent of graphical error presentation, while
controllers can respond to failures according to the user operation being
performed.

Application-level exceptions provide a stable abstraction over lower-level
failure causes while preserving the original exception chain for diagnostics.
User-facing messages can therefore remain concise without discarding technical
information useful during development or troubleshooting.

The approach introduces some explicit exception translation and handling code.
This additional structure is accepted because it makes failure behavior
predictable and prevents presentation, logging, and low-level implementation
concerns from becoming mixed.

### Current Implementation

Map loading demonstrates the complete propagation path. File access, image
decoding, metadata parsing, metadata validation, and image-processing failures
are detected within the map subsystem and represented to callers as
`MapLoadException`. Lower-level exceptions are retained as causes where
applicable.

`MapLoadController` catches the application-level failure at the user-operation
boundary, records the exception through the technical logger, and passes a
concise title and message to the view. A failed map load does not replace
previously valid map state or advance the simulation lifecycle.

Other controllers follow the same general principle where user-triggered
operations can fail: technical failures are handled at the controller boundary
rather than by model or support classes directly presenting errors.

### Related Documentation

See the `Validation and Error Handling` architecture document for the combined
validation and failure flow and `ADR 006` for the allocation of validation
responsibilities. Thread and background-task failure handling is documented in
`ADR 009`.

## ADR 008: Event log

### Status

Accepted.

### Context

The simulation contains significant domain events that are useful beyond the
immediate state in which they occur. Tasks are created, assigned, and completed;
drones enter operational phases; and the simulation itself starts, pauses,
resumes, and stops. Current model state can describe what is true at a
particular moment but does not preserve the sequence of events that produced
that state.

As monitoring and presentation features were expanded, a historical
representation of significant simulation activity became useful. This
information also needed to remain distinct from technical logging, whose purpose
is to record diagnostic information about application execution and failures.

### Problem

Deriving event history from current model state is not generally possible
because previous transitions and completed operations may no longer be
represented by that state. Allowing presentation components to construct their
own history would duplicate domain interpretation outside the model and couple
monitoring behavior to graphical presentation.

Using the technical logger as a simulation history was also inappropriate.
Technical logs describe implementation and diagnostic information, while
simulation events represent domain activity intended to be observable as part of
the application.

An unbounded event history was unnecessary for a continuously updating
simulation and could allow memory consumption to grow indefinitely.

### Decision

The application maintains a structured simulation event log as part of the
model.

Significant domain and simulation-lifecycle events are represented explicitly by
event objects containing an event type, simulation timestamp, and event-specific
descriptive information. Components responsible for the corresponding domain
operation record the event when that operation occurs.

Event timestamps use the shared simulation clock rather than wall-clock time so
that event history remains consistent with the simulated world, including
simulation pauses and speed changes.

The event log retains a bounded history rather than an unlimited record of all
events. Presentation and monitoring components consume this history as read-only
application state and do not reconstruct domain events independently.

The simulation event log remains separate from technical logging. Simulation
events describe meaningful activity within the simulated domain, while technical
logging records diagnostic information about application execution and failures.

### Alternatives Considered

Deriving historical information from current model state was rejected because
state represents the present configuration rather than the sequence of
transitions that produced it.

Recording simulation activity only through the technical logger was rejected
because diagnostic logging and domain event history serve different purposes and
have different consumers. Technical log messages are implementation-oriented,
whereas simulation events form structured application state that can be
presented and inspected.

Allowing individual presentation components to maintain their own event
histories was rejected because event ownership belongs to the simulation domain
and duplicated histories could become inconsistent.

Maintaining an unbounded event history was also rejected because the application
requires recent operational context rather than permanent event persistence.

### Consequences

Significant simulation activity has an explicit historical representation
independent of the current domain state. Monitoring components can display event
history without interpreting mutable domain objects or inferring previous
transitions.

Using structured event types provides a stable representation of event
categories, while simulation timestamps keep event history consistent with all
other time-dependent simulation behavior. Bounding the history prevents event
monitoring from introducing uncontrolled memory growth during long-running
simulations.

The principal cost is that domain operations responsible for significant events
must explicitly record them. The event log is intentionally selective rather
than a complete audit trail, so decisions are required about which state changes
and operations are significant enough to become simulation events.

### Current Implementation

The model owns a bounded `SimulationEventLog` containing `SimulationEvent`
instances classified by `SimulationEventType`. Events are timestamped using
simulation time and are added by the model components responsible for the
corresponding operation.

The event history includes significant task, drone, and simulation-lifecycle
activity. Controllers retrieve event data for presentation in the event-log view
without modifying the underlying history. The separate diagnostic monitors
derive periodically refreshed textual information from current model state
rather than from the event history.

Simulation event history and technical logging remain separate mechanisms. The
former represents observable simulation-domain activity, while the latter is
used for diagnostic information associated with application execution and
failure handling.

### Related Documentation

See the `Event Logging and Monitoring` architecture document for the
organization and presentation of simulation events. The use of simulation time
for event timestamps is documented in `ADR 003`. Technical logging associated
with failure handling is documented in `ADR 007`, while drone transition events
relate to the state-transition policy described in `ADR 005`.

<hr>

## ADR 009: Thread and lifecycle management

### Status

Accepted.

### Context

The application performs work across several execution contexts. Swing
components execute on the Event Dispatch Thread, simulation physics is advanced
by a scheduled executor, photo agencies and drones run in dedicated actor
executor pools, and additional controller operations may use background workers
for tasks that would otherwise block the user interface.

As the project evolved, simulation reset, stop, restart, and application
shutdown became increasingly important. Background workers could otherwise
continue accessing shared simulation state after the state had been cleared or
replaced, while blocking executor shutdown directly on the Swing Event Dispatch
Thread would make the graphical interface unresponsive.

### Problem

Creating background threads is insufficient without defining ownership and
termination semantics. Simulation resources must be stopped in a predictable
order, and shared state must not be reset while worker threads from an earlier
simulation remain active or are still terminating.

Pausing and stopping also represent different operations. Pausing must
temporarily suspend simulation progress without destroying worker
infrastructure, whereas stopping must prevent new work, terminate executors, and
establish a state from which the simulation may later be reset safely.

Application shutdown introduces the additional requirement that graphical
activity, exports, simulation scheduling, actors, and executor pools all
terminate without blocking the Swing Event Dispatch Thread.

### Decision

Thread ownership and lifecycle management are explicit parts of the application
architecture.

The Swing user interface is created and operated on the Event Dispatch Thread.
Simulation physics is owned by `SimulationController` and executed through a
dedicated single-threaded `ScheduledExecutorService`. Periodic graphical refresh
is driven separately by a Swing `Timer`.

The model owns the executor pools used by photo agencies and drones. Individual
actors use cooperative running and paused flags to control their worker loops,
while executor creation, shutdown, interruption, and termination remain
responsibilities of the owning model and controller components.

Stopping a simulation is treated as a coordinated lifecycle operation rather
than an immediate state reset. Sources of new work are stopped first, actors
receive stop requests, and executor termination is confirmed before the
simulation is considered fully stopped.

Blocking waits for executor termination are performed outside the Event Dispatch
Thread. Completion and user-facing results are returned to the Event Dispatch
Thread after the shutdown operation finishes.

Shared simulation state may be reset only after actor executor pools are absent
or fully terminated. This invariant prevents worker threads belonging to an
earlier simulation from accessing state that has already been cleared or reused.

Application shutdown is idempotent and follows the same ownership rules.
Controller-managed graphical and export activity is stopped before simulation
resources are shut down, and the application window is disposed only after
simulation executors have terminated successfully.

### Alternatives Considered

Allowing actor threads and executors to terminate implicitly when the
application exits was rejected because it provides no defined lifecycle for
simulation restart, reset, or controlled shutdown.

Resetting model state immediately after requesting actors to stop was rejected
because a stop request does not guarantee that the corresponding worker thread
has already terminated. This could allow stale work to access newly reset state.

Waiting synchronously for executor termination on the Event Dispatch Thread was
rejected because blocking shutdown operations would freeze the graphical
interface and prevent Swing from processing repainting or user-facing completion
and error handling.

Using only forced interruption for all worker shutdown was also rejected. Actor
loops support cooperative stopping first, while executor shutdown escalates to
interruption only when termination does not occur within the configured timeout.

### Consequences

Simulation resources now have explicit owners and termination paths. The
distinction between pausing, stopping, resetting, and final application shutdown
is reflected in the implementation rather than being treated as a single generic
cleanup operation.

Reset operations are protected against active or terminating actor executors,
reducing the risk of stale background work accessing cleared simulation state.
Executor termination is bounded by configured timeouts, and shutdown escalation
retains ownership of executors that fail to terminate rather than silently
discarding references to live resources.

Keeping blocking termination waits outside the Event Dispatch Thread preserves
graphical responsiveness during shutdown and rollback operations.

The principal cost of this approach is additional lifecycle coordination code,
intermediate states such as stopping, explicit callbacks, and asynchronous
shutdown completion. These mechanisms are considered necessary because the
application combines Swing with multiple background execution contexts that must
be coordinated safely.

### Current Implementation

`Main` creates the model, view, and root controller on Swing's Event Dispatch
Thread. `SimulationController` owns the scheduled physics executor and the Swing
GUI refresh timer, while `Model` owns the fixed-size executor pools used by
`PhotoAgency` and `Drone` actors.

Actors expose cooperative `pause()`, `resume()`, and `stop()` operations through
atomic state flags. `Model.shutdownActorPools()` first requests executor
shutdown, waits for bounded cooperative termination, escalates to
`shutdownNow()` when necessary, and clears executor references only after
confirmed termination.

`SimulationController` stops and detaches the physics executor before waiting
for its termination. Blocking waits for both actor and physics executors run
inside a `SwingWorker`, and completion handling returns to the Event Dispatch
Thread.

`Model.resetSimulation()` rejects reset while actor executor pools are still
active or terminating. Simulation startup also follows lifecycle ownership
rules: resources are created before `RUNNING` is published, and a partial
startup failure triggers rollback and executor shutdown before shared model
state is reset.

The root `Controller` coordinates final application shutdown. Repeated shutdown
requests are ignored after the first, controller-owned graphical and export
activity is stopped first, simulation shutdown is delegated to
`SimulationController`, and the application window is disposed only after
successful termination of simulation resources.

### Related Documentation

See the `Concurrency and Threading` architecture document for the execution
model and ownership of simulation threads and executors. The
`Application Lifecycle` architecture document describes startup, pause, resume,
stop, reset, rollback, and application shutdown flows. Simulation timing is
documented in `ADR 003`, while background failure handling and user-facing
shutdown errors relate to `ADR 007`.

<hr>

## ADR 010: Testing scope

### Status

Accepted.

### Context

The application combines deterministic domain logic with concurrency, image
processing, file access, simulation timing, and a Swing graphical user
interface. These areas differ considerably in how reliably and usefully they can
be exercised through automated tests.

As the original university project was refactored, automated tests became
increasingly valuable for verifying changes to simulation logic, validation,
coordinate conversion, lifecycle behavior, image processing, and other
components whose behavior can be reproduced independently of the graphical user
interface and thread scheduling.

At the same time, attempting to automate every observable application behavior
would require tests that depend on Swing rendering, asynchronous actor
execution, executor scheduling, timing, or detailed image-processing
implementation. Such tests would add significant complexity and could become
more sensitive to implementation details than to the behavior they are intended
to verify.

### Problem

The project requires sufficient automated testing to support continued
refactoring and verify important domain behavior without turning test coverage
into a separate source of complexity.

Testing only simple utility methods would leave important model behavior,
validation rules, lifecycle constraints, and failure handling unverified.
Conversely, pursuing exhaustive coverage would require brittle tests of
graphical presentation, live concurrent actors, thread scheduling, and
implementation-specific image-processing details.

A testing boundary is therefore required that identifies which behavior should
be verified automatically and which behavior is better verified through
application-level or manual testing.

### Decision

Automated testing focuses primarily on deterministic behavior that can be
isolated and reproduced reliably.

Tests cover representative domain operations, calculations, state transitions,
validation rules, boundary conditions, failure behavior, and state-preservation
guarantees. Where several closely related components form a meaningful
functional boundary, tests may exercise them together rather than requiring
every test to isolate a single class.

Time-dependent behavior is tested through simulation time and explicit physics
updates rather than through waiting for wall-clock time. Random behavior is
tested with controlled random sources where reproducibility is required.
Filesystem-dependent behavior uses temporary test resources so that loading,
metadata handling, and failure conditions can be verified without depending on
external project files.

The test suite does not attempt exhaustive verification of every implementation
detail. Swing user-interface behavior is not covered through automated GUI
testing, image-processing algorithms are tested through representative
properties and results rather than exhaustive pixel-level verification, and
concurrent simulation actors are not generally tested by relying on live thread
scheduling and timing.

These boundaries are intentional. The objective of the automated test suite is
to provide reliable regression protection for important deterministic behavior
rather than to maximize coverage metrics.

### Alternatives Considered

Relying primarily on manual testing was rejected because deterministic model
behavior can be verified more reliably and repeatedly through automated tests.
Manual testing alone would also make continued refactoring more difficult by
providing limited protection against regressions in calculations, validation,
state transitions, and failure behavior.

Pursuing exhaustive automated coverage was also rejected. GUI automation,
thread-scheduling tests, and detailed verification of implementation-specific
image-processing behavior would substantially increase test complexity while
providing limited additional confidence for the scope of this project. Such
tests could also become sensitive to execution environment and internal
implementation changes.

Restricting every automated test to a narrowly isolated class was not adopted as
an absolute rule. Some behavior is more meaningfully verified across a small
functional boundary, such as map loading together with metadata application or
model operations involving their owned supporting components.

### Consequences

The automated test suite concentrates on behavior that can be reproduced
reliably and therefore remains suitable for frequent execution during
refactoring. Important calculations, validation rules, lifecycle constraints,
state changes, and failure guarantees can be changed with immediate regression
feedback.

The approach also permits tests to verify architectural properties such as
failed operations preserving previously valid state and simulation-time behavior
remaining independent of wall-clock execution.

The principal limitation is that passing automated tests does not constitute
complete verification of the running application. Graphical presentation,
interactive workflows, visual image quality, and the complete behavior of the
concurrent simulation still require application-level observation and manual
testing.

The project therefore treats automated testing as one verification mechanism
rather than as a requirement for exhaustive system coverage.

### Current Implementation

The automated test suite uses JUnit 5 and is executed through Maven. Tests are
organized around model, domain, and support components and use nested and
parameterized tests where these structures improve readability or allow related
input cases to be expressed consistently.

Deterministic simulation behavior is tested synchronously where possible.
Simulation-time behavior is exercised by explicitly advancing model physics
rather than waiting for real time, while drone behavior is tested through
individual deterministic operations without starting the normal actor worker
thread.

Validation tests cover representative invalid values and important state
constraints. Failure-oriented tests also verify state preservation where
applicable, such as ensuring that an unsuccessful replacement map load does not
modify the previously valid map state.

Image-processing tests verify representative dimensions, regions, color
properties, and validation behavior without attempting exhaustive verification
of interpolation or every resulting pixel. File-based map tests use temporary
directories and generated image resources, while random-position behavior uses
seeded random generators where deterministic reproduction is useful.

The resulting suite provides broad regression protection for deterministic
application behavior while deliberately excluding automated Swing GUI testing
and timing-dependent verification of the complete concurrent simulation.

### Related Documentation

See the `Testing Strategy` architecture document for the organization and scope
of the current automated test suite. Deterministic simulation timing is
documented in `ADR 003`. Validation and failure behavior covered by the tests
are described in `ADR 006` and `ADR 007`, while the concurrency and lifecycle
behavior that defines the boundary of deterministic testing is documented in
`ADR 009`.

<hr>

## ADR 011: Map metadata and scale

### Status

Accepted.

### Context

The simulation uses raster map images as both the graphical representation of
the simulation environment and the spatial basis for drone movement, task
positions, and camera operations. A raster image provides pixel dimensions but
does not inherently define the real-world distance represented by those pixels
or provide descriptive and geographic information about the represented area.

As support for different map images was expanded, the application required a
consistent way to associate a map with information such as title, source,
license, attribution, physical scale, and optional geographic reference.
Different parts of this information originate from different sources. Image
dimensions are derived from the loaded raster image, while descriptive and
spatial information may be supplied through an external metadata file or
configured manually by the user.

### Problem

Simulation movement and distances require a defined relationship between image
pixels and real-world units. Without an explicit scale, drone speed, travel
distance, camera coverage, safe map margins, and task positions would depend on
arbitrary image resolution rather than representing meaningful physical
distances.

Map metadata also combines information with different origins and different
validation requirements. Treating external metadata as the complete
authoritative representation of a map would mix file-provided information with
properties derived from the loaded image. At the same time, requiring every map
to provide complete external metadata would unnecessarily prevent ordinary
raster images from being used in the simulation.

A consistent model was therefore required for combining image-derived
properties, optional external metadata, configured defaults, and manual spatial
overrides while preserving the validity of the currently loaded map.

### Decision

The application represents the active map through dedicated runtime metadata
that combines image-derived properties with optional descriptive and spatial
information.

Map scale is defined as the number of real-world meters represented by one pixel
of the cropped world image. Simulation positions and distances are expressed in
meters where they represent domain state, while conversions between meters,
world-image pixels, and display pixels are performed by the map model.

Optional map metadata may be supplied through a JSON sidecar file associated
with the raster image. External metadata is first deserialized into a separate
file representation and is then applied to the runtime map metadata. This keeps
the structure of external metadata separate from properties that are derived
from the loaded and processed image.

A map does not require external metadata in order to be usable. When no scale is
provided by file metadata, the application assigns a configured default scale.
The source of the active scale is retained so that the application can
distinguish between a default value, a value supplied by file metadata, and a
manual user override.

Optional geographic reference information is maintained separately from map
scale and records its own source. Geographic coordinates therefore provide
additional information about the represented map without becoming a requirement
for simulation coordinates or distance calculations.

Metadata and image processing form part of the same map-loading operation.
Image-derived properties and optional file metadata are processed and validated
before the resulting map state replaces the currently active map.

### Alternatives Considered

Using image pixels directly as simulation coordinates was rejected because
physical simulation behavior would then depend on raster resolution. Drone
speeds and travel distances would have no consistent real-world meaning across
maps with different pixel dimensions or scales.

Requiring every map to provide complete metadata was rejected because scale and
geographic information are not always available and are not required for the
basic simulation. A configured default scale allows ordinary raster images to
remain usable while still giving simulation distances a defined physical
meaning.

Using the deserialized sidecar metadata directly as the runtime map
representation was also rejected because the external file does not contain all
information required by the application. Image dimensions and processed world
dimensions originate from the raster image itself and therefore belong to the
runtime representation rather than to the external metadata format.

Embedding scale and geographic assumptions throughout the simulation model was
rejected in favor of centralizing coordinate conversion and map-specific spatial
information within the map subsystem.

### Consequences

Simulation distances and movement have a defined physical meaning independent of
the resolution used to display the map. Domain objects can represent positions
in meters without depending directly on raster or graphical presentation
coordinates.

Maps can provide richer descriptive and spatial information through optional
sidecar metadata while remaining usable when that information is absent.
Tracking the source of scale and geographic reference values also preserves the
distinction between configured defaults, file-provided information, and manual
overrides.

Separating external file metadata from runtime metadata prevents the JSON
representation from becoming coupled to image-derived application state. It also
allows metadata validation and image processing to complete before the active
map is replaced, preserving previously valid state when a replacement map fails
to load.

The approach introduces additional metadata classes, provenance information, and
coordinate conversions. It also requires a clear distinction between world-image
pixels, display pixels, and world-meter coordinates throughout the map
subsystem. This additional structure is accepted because it prevents simulation
behavior from becoming dependent on image resolution or graphical scaling.

### Current Implementation

`MapModel` owns the loaded map images, runtime `MapMetadata`, map scale, and
coordinate conversions. The original raster image is processed into a cropped
world image and a separately resampled display image, allowing simulation-space
coordinates and graphical presentation coordinates to remain distinct.

`MapMetadataLoader` reads optional JSON sidecar metadata into
`MapFileMetadata`. `MapModel` combines this information with dimensions derived
from the processed raster image to construct the runtime `MapMetadata`.
File-provided scale and geographic information are validated before being
applied.

The active scale is expressed as meters per world-image pixel and may originate
from the configured default, file metadata, or a manual user override.
`MapMetadata` records the corresponding source. Optional upper-left geographic
coordinates and coordinate-reference-system information are maintained
separately and similarly distinguish file-provided and manually supplied
georeference information.

`MapModel` provides conversions between world-image pixels, display pixels, and
world-meter coordinates. Simulation-domain positions such as drone bases, drone
positions, and task targets are represented in world meters, while conversion to
display coordinates occurs when those positions are prepared for graphical
presentation.

Map loading is transactional. Processed images and metadata are prepared and
validated before the corresponding `MapModel` fields are replaced, so a failed
load does not partially replace an already valid map.

### Related Documentation

See the `Map and Coordinate System` architecture document for the current map
representation, coordinate spaces, scale conversions, and map-processing flow.
Validation of map metadata and transactional map loading are described in the
`Validation and Error Handling` architecture document and motivated by
`ADR 006` and `ADR 007`. The separation between simulation-domain coordinates
and presentation data also relates to the model-view boundary documented in
`ADR 004`.


