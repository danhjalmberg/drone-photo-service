# Architecture

This document describes the current architecture of the Drone Photo Service
application. It explains the organization of the application, the
responsibilities of its principal subsystems, the flow of data and control
between them, and the invariants that define their boundaries.

The document describes how the current implementation works rather than the
historical reasoning that led to it. Significant architectural choices and the
alternatives considered during development are documented separately in the
Architecture and Design Decisions document. The broader progression from the
original university project to the current implementation is summarized in the
Project Evolution document.

The sections are intentionally organized by architectural concern rather than by
source-code package. Individual classes may therefore appear in several sections
when they participate in more than one subsystem.

## Contents

1. [Application Architecture](#application-architecture)
2. [Controllers](#controllers)
3. [Simulation Model](#simulation-model)
4. [Simulation Time](#simulation-time)
5. [Snapshots and View Data](#snapshots-and-view-data)
6. [Drone State Machine](#drone-state-machine)
7. [Task Processing](#task-processing)
8. [Concurrency and Threading](#concurrency-and-threading)
9. [Application Lifecycle](#application-lifecycle)
10. [Map and Coordinate System](#map-and-coordinate-system)
11. [Image Processing](#image-processing)
12. [Validation and Error Handling](#validation-and-error-handling)
13. [Event Logging and Monitoring](#event-logging-and-monitoring)
14. [Testing Strategy](#testing-strategy)

[//]: # (Each record should answer the question How?)

<hr>

## Application Architecture

### Purpose

The application is organized according to the Model–View–Controller (MVC)
architectural pattern. The objective is to separate simulation logic, graphical
presentation, and application coordination into components with distinct
responsibilities and well-defined dependency directions. This separation reduces
coupling between the user interface and the simulation domain, improves
maintainability, simplifies testing of deterministic model behavior, and allows
individual subsystems to evolve independently.

### Architectural Structure

The application consists of one shared `Model`, one shared `View`, and a
controller layer that mediates all communication between them. The application
entry point constructs the shared model and view and injects them into the root
controller, which composes the remaining controller layer and establishes the
application wiring.

The model owns the simulation state and domain logic. The view implements the
graphical user interface. The controller layer translates user interactions into
model operations and coordinates presentation updates.

### Component Responsibilities

The `Model` owns the complete simulation state together with the application's
domain logic. It manages simulation actors, tasks, map data, simulation time,
simulation event history, archives, and application-wide state. The model
contains no dependencies on Swing components or controller classes.

The `View` implements the graphical user interface using Swing. It constructs
the user interface, exposes user interactions through listener registration
methods, presents application state, and displays dialogs. The view performs no
simulation logic and does not modify model state directly.

The controller layer coordinates all interaction between the model and the view.
Controllers receive user input from the view, invoke model operations,
coordinate application state, and update the presentation with the resulting
application state.

### Dependency Rules

Dependencies are intentionally unidirectional.

The model has no knowledge of the controller layer or the graphical user
interface.

The view has no knowledge of model implementation details and interacts with the
application only through listener registration methods and display operations.

Controllers depend on both the model and the view and therefore provide the only
communication path between them.

### Data Flow

User interactions originate in the view and are delivered to the controller
layer through registered listeners. Controllers interpret the requested
operation, invoke the appropriate model functionality, retrieve the resulting
application state, prepare presentation-specific data where required, and update
the graphical user interface.

This separation allows simulation logic to remain independent of graphical
presentation while allowing the view to remain independent of simulation
implementation.

### Architectural Invariants

The architecture is governed by several invariants. The model never depends on
Swing classes or controller implementations. The view never modifies model state
directly. Controllers provide the only communication path between the model and
the view. Presentation-specific objects define the interface between the
simulation domain and graphical rendering. Responsibilities are assigned
according to application behavior rather than graphical layout, allowing
individual subsystems to evolve independently while preserving clear ownership
of responsibilities.

### Related Decisions

The architectural rationale for adopting and refining this organization is
documented in `ADR 001`. The internal organization of the controller layer is
described in the `Controllers` architecture document and motivated by `ADR 002`.
The use of presentation snapshots and view-specific data across the model–view
boundary is documented in `ADR 004`.

<hr>

## Controllers

### Purpose

The controller layer coordinates all interaction between the model and the
graphical user interface. Controllers receive user input from the view, invoke
model operations, coordinate application state, and update the presentation with
the resulting data. They provide the only communication path between the model
and the view and therefore define the application's operational behavior.

### Organization

The controller layer is organized around a root `Controller` together with a
collection of specialized controllers. Each specialized controller is
responsible for one coherent area of application behavior rather than for
individual user interface components or individual model classes.

The root `Controller` acts as the application's composition point. It constructs
the controller graph, establishes dependencies between controllers, registers
listeners with the view, and coordinates application startup and shutdown.
Operational behavior is delegated to specialized controllers.

### Responsibilities

The controller layer separates application behavior into a number of distinct
responsibilities.

`SimulationController` manages the simulation lifecycle, including simulation
creation, execution, pausing, resuming, stopping, scheduling of simulation
updates, and coordinated shutdown of background activity.

`ViewRefreshController` synchronizes presentation state with the model. It
retrieves snapshots and other read-only model data, prepares
presentation-specific view data, and updates the graphical user interface during
refresh cycles.

`SelectionController` maintains the application's selection state. It
synchronizes selections between the map, overview tables, thumbnail strip, and
details panels while coordinating task playback and detail updates.

`ControlStateController` determines which commands and user interface controls
are enabled for the current application state, ensuring that available actions
remain consistent with the simulation lifecycle.

Feature-specific workflows such as map loading, archive viewing, task playback,
image export, and command dispatch are implemented by dedicated controllers
responsible for those individual concerns.

### Collaboration

Controllers collaborate through explicit dependencies and narrowly scoped
callbacks. Each controller owns its own behavior while invoking other
controllers only where coordination between application concerns is required.
Shared application state remains within the model rather than being duplicated
across controllers.

The controller layer is responsible for translating user interactions into model
operations and for translating model state into presentation updates. This
separation allows the model to remain independent of graphical presentation
while allowing the view to remain independent of simulation logic.

### Threading

Controllers provide the synchronization point between background simulation
activity and the Swing Event Dispatch Thread. Background work is initiated and
coordinated through the controller layer, while graphical updates are performed
using presentation-specific data obtained from the model. This keeps thread
coordination outside both the model and the graphical user interface.

### Design Principles

The controller layer follows a small number of architectural principles. Each
controller owns a coherent area of application behavior rather than a particular
user interface component or model class. Controllers coordinate application flow
without implementing simulation logic, and persistent application state remains
within the model. Communication between controllers is performed through
explicit dependencies rather than global state or bidirectional relationships.
The root controller remains responsible for application composition, while
operational behavior is delegated to specialized controllers.

### Related Decisions

The architectural rationale for introducing specialized controllers is
documented in `ADR 002`. The overall MVC organization is described in the
`Application Architecture` document, while presentation snapshots and
view-specific data are documented in `ADR 004`.

<hr>

## Simulation Model

### Purpose

The simulation model represents the domain state and behavior of the drone photo
service. It coordinates photo agencies, drones, tasks, the shared task queue,
completed task storage, map state, simulation time, and simulation events.

`Model` provides the main boundary between the controller layer and the
simulation domain. It owns or coordinates the principal model components but
does not implement the detailed behavior of individual simulation actors.

### Model Organization

`Model` maintains the active collections of photo agencies and drones together
with the shared `TaskQueue` and the `TaskArchive`. It also owns the `MapModel`,
simulation clock state, event log, actor executor pools, and configuration
required to create and control simulation actors.

Photo agencies and drones retain their own operational state and behavior.
`PhotoAgency` is responsible for producing and receiving tasks, while `Drone`
is responsible for task acquisition, movement, battery-dependent behavior, and
task execution. `Model` coordinates these objects and exposes their state to the
controller layer through model operations and read-only snapshots.

Actor creation validates the corresponding executor before changing model
state. Registration and executor submission occur under the model lock; a
rejected submission removes the provisional actor and does not consume its
identifier. Other model operations therefore cannot observe an actor that was
never accepted for execution.

This organization keeps simulation-specific behavior within the domain model
while providing controllers with a single primary interface to that model.

### Simulation Actors

Photo agencies represent task producers. Each agency creates tasks at
simulation-time intervals and attempts to place them in the shared task queue.
Task creation includes the task type, creation time, originating agency, and a
safe target position expressed in world meters.

Drones represent task consumers and mobile simulation actors. Each drone is
assembled from a battery, camera, and motor and maintains its base position,
current position, current task, operational state, and lightweight history of
completed tasks.

When `Model` adds a drone, it selects from `DroneType.values()` so the model does
not maintain a duplicated list of supported configurations. `DroneFactory`
uses an exhaustive switch expression to associate `TYPE_1`, `TYPE_2`, and
`TYPE_3` with compatible `Assembly` implementations. Each assembly is an
Abstract Factory that creates a fixed battery, camera, and motor family. The
factory passes those components to the package-private `Drone` constructor, so
every created drone is fully initialized and external code cannot bypass the
factory. A null `DroneType` or component is rejected with
`NullPointerException`. The enum provides explicit display and serialized
values for presentation, configuration, or external-output boundaries without
relying on `Enum.toString()`.

Actor behavior is divided between centrally triggered simulation updates and
actor-owned worker execution. `Model.updatePhysics()` advances simulation time
and supplies the same simulation step and timestamp to photo agencies and
drones. Work that should not block this update path is executed by the actor
worker threads. The detailed execution and synchronization model is described in
the `Simulation Time` and `Concurrency and Threading` sections.

### Task Flow

`Task` represents a unit of work created by a photo agency and executed by a
drone. A task records its originating agency, type, target position, simulation
timestamps, generated images, and image capture positions.

New tasks are passed through the shared bounded `TaskQueue`. Photo agencies
produce tasks and drones consume available tasks from the same queue, forming
the producer-consumer boundary between the two actor types.

After successful execution, the drone returns the completed task to the photo
agency that created it. The agency records the completion time and transfers the
task to the central `TaskArchive`. If execution is aborted because the drone
cannot continue safely, the task is instead returned to its originating agency,
its execution state is reset, and the agency attempts to enqueue it again.

The processing differences between photo, video, and zoom tasks are described in
the `Task Processing` section.

### Spatial and Physical State

Simulation positions are represented by immutable `Vector2D` values using
double-precision coordinates. Drone bases, drone positions, task targets, and
image capture positions are stored in world meters rather than raster or display
pixels.

Drone movement uses motor speed expressed in meters per second and the simulated
time step supplied by the central physics update. Battery state is represented
as remaining operating time and is consumed or restored according to simulated
elapsed time.

The map subsystem provides the conversion between physical simulation
coordinates and raster coordinates when image operations require it. This keeps
movement and task positioning independent of image and display resolution. The
coordinate spaces and conversion rules are described in the `Map and Coordinate
System` section.

### Completed Task Ownership

Completed tasks can contain significant image data. Long-term ownership of these
tasks is therefore centralized in `TaskArchive` rather than duplicated across
simulation actors.

A drone retains only lightweight `TaskSnapshot` markers for its recently
completed tasks. The full `Task` objects, including their generated images, are
stored by the bounded archive. When archived tasks are removed because the
configured capacity is exceeded, their image references are explicitly cleared
before the tasks are discarded.

The archive also provides the model-side source for task details, thumbnails,
and export data. Presentation-specific representations are created from the
archived state rather than exposing the archive as mutable view state.

### Model State Access

Controllers interact primarily with `Model` rather than directly coordinating
individual simulation actors. `Model` exposes operations for simulation
configuration and lifecycle control and provides snapshots or defensive copies
for state that is consumed outside the domain model.

Mutable actor collections remain internal to the model. When collections must be
processed while actors may be active, the model creates temporary copies under
synchronization before operating on them. Task and actor state intended for the
view is converted into dedicated snapshot representations.

This boundary complements the controller and snapshot architecture described in
the `Controllers` and `Snapshots and View Data` sections.

### Important Invariants

Simulation-domain positions use world meters and remain independent of display
resolution. Simulation progression uses elapsed simulation time rather than wall
clock timestamps.

Tasks move through defined ownership stages from their originating photo agency,
through the shared queue and a processing drone, and back to the originating
agency before completed tasks enter the archive. Aborted tasks are reset before
being made available for another execution attempt.

Full completed task results are retained by the bounded task archive. Drones
retain only lightweight completion markers and do not become long-term owners of
task image data.

Actor collections and executor resources cannot be reset while actor executors
remain active or are still terminating. Detailed lifecycle constraints are
described in the `Application Lifecycle` section.

### Related Classes

`Model`, `PhotoAgency`, `Drone`, `DroneType`, `DroneFactory`, `Assembly`, `Task`,
`PhotoTask`, `VideoTask`, `ZoomTask`,
`TaskQueue`, `TaskArchive`, `Battery`, `Camera`, `Motor`, `Vector2D`,
`MapModel`

### Related Decisions

The separation between the model and controller layer is documented in
`ADR 001` and `ADR 002`. Simulation-time progression is documented in
`ADR 003`, and model-to-view snapshots are documented in `ADR 004`. Drone state
transitions are documented in `ADR 005`. Actor concurrency and lifecycle
ownership are documented in `ADR 009`. Spatial metadata and physical map scale
are documented in `ADR 011`.

<hr>

## Simulation Time

### Purpose

Simulation time provides the authoritative representation of temporal progress
within the simulated world. It defines the progression of all time-dependent
simulation behavior independently of wall-clock execution speed and provides a
consistent temporal reference for the entire application.

### Responsibilities

The simulation clock maintains the current simulated time and advances only as
part of the simulation update cycle. Time-dependent model components obtain
temporal information from this shared simulation time rather than directly from
the operating system.

Simulation time is used to coordinate simulation progress, task timestamps,
battery consumption, event history, and other domain concepts whose behavior
depends on the progression of the simulated world.

### Main Components

The simulation clock is owned by the model and represents the single source of
temporal information for the simulation. The centralized physics update advances
the clock according to its simulation time step, while controllers and model
components consume the resulting simulation time when performing time-dependent
operations.

Presentation components display simulation time but do not modify it.

### Data Flow

The simulation update cycle advances the shared simulation clock. Model
components consume the updated simulation time while performing domain
operations.

Controllers retrieve simulation time from the model when coordinating
application behavior and when constructing presentation state.

Temporal information therefore flows from the simulation clock through the model
to the controller layer and finally to the presentation.

### Threading Considerations

Simulation time advances only during simulation updates and therefore remains
synchronized with the simulation state. Controllers and presentation components
treat simulation time as read-only. This ensures that all threads observe a
consistent notion of simulated time while preventing graphical refresh frequency
from influencing simulation progress.

### Important Invariants

The simulation clock is the single authoritative source of temporal information
within the simulation domain. Simulation time advances only as part of the
simulation update cycle. Pausing the simulation also pauses the progression of
simulated time. Components within the simulation domain do not obtain elapsed
time directly from the operating system. Wall-clock time is used only for
interactions with external systems, such as timestamping exported files.

### Related Classes

`Model`, `SimulationController`, `SimulationClock`, `Task`, `SimulationEvent`,
`TaskImageExporter`

### Related Decisions

The architectural rationale for introducing simulation time is documented in
`ADR 003`. The interaction between simulation time and the event log is
described in `ADR 008`, while its role in deterministic testing is documented in
`ADR 010`.

<hr>

## Snapshots and View Data

### Purpose

Snapshots and view-data objects define the interface between the simulation
domain and the graphical presentation. They provide read-only,
presentation-oriented representations of application state, allowing the user
interface to render simulation data without depending directly on live domain
entities.

### Responsibilities

The model is responsible for producing snapshots that represent the current
simulation state. Controllers retrieve these snapshots, perform
presentation-specific transformations where required, and pass the resulting
view-data objects to the view.

The view consumes snapshots and view-data objects exclusively for graphical
presentation. It does not access mutable simulation objects or derive
presentation state directly from the model.

### Main Components

Snapshots represent read-only views of simulation entities such as drones,
tasks, and application state. View-data objects represent presentation-specific
structures prepared for individual user interface components, including the map,
header, status bar, task details, thumbnails, and other graphical elements.

Together, snapshots and view-data objects provide a stable representation of the
information required by the graphical user interface while remaining independent
of Swing implementation details.

### Data Flow

The simulation updates the domain model. When the graphical user interface is
refreshed, controllers request snapshots from the model representing the current
simulation state. Controllers then construct view-data objects where
presentation-specific transformation or aggregation is required before updating
the view.

Presentation data therefore flows in one direction, from the model through the
controller layer to the graphical user interface.

### Threading Considerations

Snapshots and view data are treated as read-only after creation. Scalar values
and immutable value objects are captured directly. Drone component state is
represented by immutable battery, camera, and motor snapshots. Some image-based
representations retain shared image references to avoid expensive deep copies;
those images must not be mutated by the presentation layer.

This separation reduces coupling between simulation updates and rendering. It
does not by itself guarantee an atomic multi-field snapshot: snapshot creation
must still synchronize with the domain object that owns the captured state.

### Important Invariants

Mutable simulation objects remain internal to the model. Presentation components
operate exclusively on read-only snapshots and presentation-specific view-data
objects. The model is responsible for producing snapshots, while controllers
remain responsible for presentation-specific transformations. The view neither
modifies snapshots nor derives presentation state directly from mutable
simulation objects.

### Related Classes

`DroneSnapshot`, `TaskThumbnailSnapshot`, `TaskDetailsSnapshot`,
`SimulationHeaderViewData`, `StatusBarViewData`, `MapDroneViewData`,
`MapTaskViewData`, `ViewRefreshController`

### Related Decisions

The architectural rationale for introducing snapshots is documented in
`ADR 004`. The overall model–view boundary is described in the
`Application Architecture` document, while the responsibilities of the
controller layer are documented in the `Controllers` architecture document.

<hr>

## Drone State Machine

### Purpose

The drone state machine represents the current operational phase of each drone
and provides an explicit lifecycle model for its simulation behavior. Drone
state is used to distinguish waiting, charging, movement, task processing, and
return behavior while providing a stable representation of the drone's current
activity to monitoring and presentation components.

### Responsibilities

The `Drone` domain object owns its current state and determines when state
transitions occur. State changes are derived from simulation conditions such as
current position, task assignment, battery capacity, and task-processing
progress.

The state value describes the resulting lifecycle condition but does not
independently drive the simulation. Movement, battery management, task
processing, and task assignment remain implemented by the corresponding drone
domain operations.

### Main Components

The current state is represented by `DroneState` and stored internally by
`Drone`. State modification is centralized through `transitionStateTo()`, which
changes the state only when the requested state differs from the current state
and reports whether an actual transition occurred.

The drone lifecycle currently includes idle, charging, movement toward a task,
task processing, and return-to-base behavior. The transition conditions are
implemented in the drone's position-dependent and task-processing operations
rather than in a separate state-machine framework.

### State Flow

A drone begins in the idle state. At the base position, insufficient charge
moves the drone into the charging state. Once fully charged, an idle drone can
acquire a task and begin moving toward its target. Movement toward an assigned
task places the drone in the moving-to-task state.

When the drone reaches the task position and processing begins, it enters the
processing-task state. It remains there while the minimum hover duration and
asynchronous task-result work are incomplete. After successful task completion,
the task state is cleared and the drone returns to idle.

If available battery becomes insufficient for continued operation, the current
task is discarded where necessary and the drone enters the returning-to-base
state. On reaching the base, it either begins charging or becomes idle when
fully charged.

### Transition Handling

State transitions are requested by the domain operation responsible for the
corresponding behavior. `moveToTask()` requests the moving-to-task state,
`startTaskProcessing()` requests the processing-task state, `returnToBase()`
requests the returning-to-base state, and base-position behavior selects
charging or idle state according to battery condition.

The transition operation returns whether the state actually changed. This allows
transition-specific side effects, such as simulation events, to occur only when
entering a state rather than on every physics update while remaining in that
state. Charging, task-start, and return-to-base events use this behavior.

### Threading Considerations

Drone lifecycle decisions are made during the centralized physics update through
`performSimulation()`, which is synchronized. Movement, battery consumption,
transition decisions, and completion checks therefore follow the physics update
cycle.

CPU-intensive task-result work is delegated to the drone's worker thread.
Completion flags communicate the result of that work back to the
physics-controlled lifecycle. The drone remains in the processing-task state
until both the asynchronous work and the simulated minimum processing duration
have completed.

### Important Invariants

The drone owns its lifecycle state and external components do not modify it
directly. State changes occur only through the dedicated transition operation. A
state transition is recorded only when the requested state differs from the
current state. State-dependent side effects that should occur once on entry are
therefore tied to actual transitions rather than repeated simulation ticks.

Task-result work may execute asynchronously, but completion of the task
lifecycle remains controlled by the physics update. A drone does not complete a
task until the required simulated hover duration has elapsed and the
corresponding worker-thread result has completed.

### Related Classes

`Drone`, `DroneState`, `Task`, `Battery`, `Motor`, `SimulationEventType`

### Related Decisions

The rationale for centralizing drone state changes is documented in `ADR 005`.
Simulation timing of state-dependent behavior is described in `ADR 003`, while
the division between physics updates and background actor work is described in
the `Concurrency and Threading` architecture document.

<hr>

## Task Processing

### Purpose

Task processing defines the lifecycle of simulation work from creation by a
photo agency to execution by a drone and final storage in the task archive. It
coordinates task production, queueing, assignment, movement, image acquisition,
completion, and recovery when a task cannot be completed.

The processing model supports photo, video, and zoom tasks while retaining a
common task representation and lifecycle. Differences between task types are
implemented primarily in drone processing behavior rather than through separate
task execution frameworks.

### Task Creation

Photo agencies produce tasks according to simulation-time production intervals.
When task production is due, the agency delegates creation and queueing work to
its actor thread rather than performing that work directly in the centralized
physics update.

A new `TaskType` is selected from `PHOTO`, `VIDEO`, and `ZOOM` and passed to
`TaskFactory`, whose exhaustive switch creates the corresponding `PhotoTask`,
`VideoTask`, or `ZoomTask`. The creating agency then assigns the task its
simulation creation time, name, originating agency, and target position.

Task identity remains a `TaskType` throughout the model, snapshots, playback,
export, and map-rendering data. Presentation code uses the enum's explicit
display name, while external output uses its explicit serialized value rather
than relying on `Enum.toString()`.

Task targets are generated within the valid simulation area and stored in world
meters. The task therefore remains independent of raster and display coordinates
throughout its domain lifecycle.

After creation, the task is offered to the shared bounded `TaskQueue`. If the
queue cannot currently accept the task, the agency retains it and attempts to
enqueue it during a subsequent production update rather than discarding it.

### Task Assignment

Available drones retrieve tasks from the shared task queue. Assignment transfers
the task from the producer-consumer queue to the drone that will execute it.

The drone records the task's target position and processing start time and then
moves toward the target according to its normal movement and battery behavior.
Task ownership remains associated with the photo agency that originally created
the task so that completed or aborted work can later be returned to the same
producer.

For video tasks, result acquisition begins during movement toward the target.
Photo and zoom tasks perform their image acquisition after reaching the target
position.

### Processing Coordination

Task execution is coordinated by the drone's centralized physics behavior. When
the drone reaches the target, the task type determines which processing path is
used.

Starting target-position processing places the drone in the processing-task
state, stops its motor, initializes the required minimum hover duration, and
requests any result-generation work that should execute asynchronously.

The drone continues to hover at the target while the minimum simulated
processing duration is reduced by physics updates. Battery consumption during
this period is therefore based on simulation time rather than on the wall-clock
duration required to generate the result.

CPU-intensive image work is delegated to the drone's actor thread. Completion
flags communicate the result of this work back to the physics-controlled
lifecycle. A task is considered ready to finish only when both the asynchronous
result work and the required simulated processing duration have completed.

### Photo Tasks

A photo task requires the drone to travel to the target and capture one
high-resolution image at that position.

When processing begins, the drone delegates image capture to its actor thread.
The map image is sampled at the task position and the drone's camera applies its
configured image filter. The resulting image and its capture position are stored
in the task.

The detailed raster extraction, interpolation, and camera filtering operations
are described in the `Image Processing` architecture section.

### Video Tasks

A video task records the drone's movement from task assignment toward the target
rather than representing only an operation performed after arrival.

During each movement step, the drone determines frame positions along the
traveled segment according to the configured video frame rate. Intermediate
capture positions are interpolated between the previous and current drone
positions, allowing frame generation to remain largely independent of the
granularity of the physics update.

The corresponding image frames and world-meter capture positions are stored in
the task. Video storage is bounded, and when its frame limit is reached the task
retains the newest frames rather than allowing image storage to grow without
limit.

After reaching the target, the drone performs the remaining target-position
processing and completes the task according to the same processing lifecycle
used for other task types.

### Zoom Tasks

A zoom task requires the drone to travel to the target, capture the target
region, and generate a sequence representing progressively closer views.

Result generation occurs through the drone's asynchronous task-processing work
after the target has been reached. The generated frames are stored as the task's
image result together with the corresponding capture-position information.

The zoom sequence is bounded by the configured maximum frame count. The
resampling operations used to construct the sequence are part of the image
processing subsystem and are described separately in the `Image Processing`
architecture section.

### Camera Processing

Each drone contains a `Camera` implementation that determines the image filter
applied to captured source imagery. The camera abstraction supports color,
grayscale, and negative output while exposing the same filtering operation to
task-processing behavior.

Task type and camera type therefore represent separate concerns. The task
determines what imagery must be acquired and when, while the camera determines
how captured imagery is transformed before being stored as a task result.

This allows the same task-processing lifecycle to operate with different drone
camera configurations without introducing camera-specific behavior into the task
classes.

### Task Completion

When the required result work and simulated processing duration have completed,
the drone finalizes the current task. The completed task is returned to the
`PhotoAgency` that originally created it.

The agency records the completion simulation time and transfers the full task to
the model's central `TaskArchive`. The archived task retains its timestamps,
generated images, and image capture positions for later inspection, playback,
and export.

The drone does not retain the completed `Task` as long-term result storage.
Instead, it keeps a lightweight `TaskSnapshot` containing identifying and
spatial information required for its recent completion history.

### Task Abortion and Requeueing

Battery availability is evaluated as part of drone operation. If a drone cannot
continue a task while preserving sufficient battery for its required movement,
the current task is aborted and returned to its originating photo agency.

Before the task can be attempted again, its execution-specific state is reset.
Processing timestamps, generated images, and stored image positions are cleared,
while the task identity, originating agency, creation information, type, and
target remain associated with the original task.

Returned tasks are retained by the photo agency and receive priority over newly
created tasks when the agency next has an opportunity to enqueue work. The same
logical task can therefore be executed again by another available drone without
retaining partial results from the failed attempt.

### Task Result Storage

`Task` provides the common storage for task timestamps, target position,
generated images, and image capture positions. Image collections are bounded
according to task type so that task execution cannot introduce unbounded image
storage.

Photo tasks retain their limited photographic result, while video and zoom tasks
store bounded frame sequences. Video tasks differ in that their bounded
collections retain newer frames when capacity is reached, reflecting their
continuous acquisition during movement.

Completed full task results are ultimately owned by `TaskArchive`. Presentation
components access task information through snapshots and other read-only
representations rather than through direct mutation of archived tasks.

### Threading Considerations

Task processing spans the centralized physics update and actor execution
contexts. Simulation-time decisions, movement, battery consumption, processing
duration, task-state transitions, and completion conditions are controlled by
the physics update.

Photo-agency threads perform task creation and queue insertion, while drone
threads perform delegated result-generation work that should not block physics
progression. The shared `TaskQueue` provides the producer-consumer boundary
between these actor types.

Task image and capture-position collections synchronize their mutable storage
because result data may be produced by actor work while task state is observed
from other execution contexts.

The complete execution model and ownership of these threads are described in the
`Concurrency and Threading` architecture section.

### Important Invariants

Every task retains its originating photo agency throughout its lifecycle.
Completed and aborted tasks are returned to that agency rather than being
transferred directly from a drone to another producer.

Task targets and image capture positions are represented in world meters.
Raster-coordinate conversion occurs only when image acquisition requires access
to the map image.

Task completion is controlled by simulation behavior rather than by the
wall-clock duration of image processing. A target-position task does not finish
until its required simulated processing duration and asynchronous result work
have both completed.

Aborted tasks do not retain partial execution results. Their execution
timestamps, images, and capture positions are reset before they are made
available for another attempt.

Generated image collections remain bounded according to task type. Full
completed results are owned by the central task archive, while drones retain
only lightweight completion snapshots.

### Related Classes

`Task`, `TaskType`, `PhotoTask`, `VideoTask`, `ZoomTask`, `TaskFactory`, `TaskQueue`,
`TaskArchive`, `PhotoAgency`, `Drone`, `Camera`, `TaskSnapshot`

### Related Decisions

Simulation-time control of task creation and processing is documented in
`ADR 003`. The use of read-only task representations outside the domain model is
documented in `ADR 004`. Drone lifecycle transitions during assignment,
processing, and return behavior are documented in `ADR 005`. Actor execution,
producer-consumer coordination, and task-processing thread ownership are
documented in `ADR 009`. Spatial task positions and their relationship to map
imagery are documented in `ADR 011`.

<hr>

## Concurrency and Threading

### Purpose

The application separates simulation progression, actor work, and graphical
presentation across distinct execution contexts. This prevents simulation and
CPU-intensive work from blocking the Swing Event Dispatch Thread while
preserving a centralized update cycle for time-dependent simulation behavior.

### Execution Model

Swing components execute on the Event Dispatch Thread. The application model,
view, and root controller are initially constructed on this thread, and
graphical updates are performed through Swing-managed execution.

Simulation physics executes independently on a single-threaded scheduled
executor owned by `SimulationController`. This executor advances simulation time
and coordinates recurring simulation behavior such as drone movement, battery
updates, task-production countdowns, and submission of actor work.

Photo agencies and drones execute through separate fixed-size executor pools
owned by the model. Each actor implements `Runnable` and maintains its own work
queue for operations that should execute in its actor thread.

Periodic graphical refresh is independent of the physics update. A Swing `Timer`
triggers controller-driven presentation updates on the Event Dispatch Thread,
allowing simulation frequency and graphical refresh frequency to be configured
separately.

### Centralized Physics Updates

`SimulationController` uses a single-threaded `ScheduledExecutorService` to
invoke the model physics update at a fixed rate. The model advances the shared
simulation clock and updates simulation actors using the resulting time step.

The physics update is responsible for deterministic progression of simulation
state rather than CPU-intensive task-result generation. Drone movement, battery
consumption, state-transition conditions, photo-agency production countdowns,
and decisions to enqueue actor work are therefore driven from the centralized
update cycle.

Using a single physics execution context provides a common temporal progression
for these operations and prevents graphical refresh timing from controlling
simulation behavior.

### Actor Execution

Photo agencies and drones execute independently of the centralized physics
thread. The model creates separate fixed-size executor pools for each actor type
and submits the actor instances to those pools.

Each actor maintains a bounded internal `BlockingQueue<Runnable>` containing
work that must execute in that actor's thread. The actor run loop polls its
queue and executes submitted work while respecting its running and paused state.

For photo agencies, the physics update determines when task-production work
should be requested, while creation and queueing work executes in the
photo-agency thread. For drones, the physics update controls movement, battery
behavior, state transitions, and task-completion conditions, while CPU-intensive
task-result work executes in the drone thread.

This separates simulated progression from computational work without allowing
actor execution speed to define simulation time.

### Educational Threading Model

The actor-based threading model is intentionally more explicit than would
normally be required for an application of this scale. Photo agencies and
drones are represented as independently executing `Runnable` actors, with
executor-managed worker threads and blocking queues, primarily to demonstrate
producer-consumer communication, synchronization, asynchronous work, and
thread-lifecycle management.

This design should therefore not be interpreted as an attempt to maximize CPU
utilization by assigning one application thread to each simulation entity. A
production implementation with large numbers of actors would normally use a
different execution strategy, such as shared worker pools, task-based
parallelism, or another concurrency model appropriate to the workload.

An earlier consequence of coupling actor behavior too closely to independently
scheduled threads was that simulation timing could be influenced by operating
system scheduling and real-time delays. In particular, using actor-thread
execution intervals to advance drone movement makes simulated speed dependent
on how regularly those threads actually run.

The current architecture avoids that dependency by separating simulation time
from actor-thread scheduling. Drone movement, battery consumption, production
countdowns, and lifecycle decisions are advanced by the centralized fixed-step
physics update. Actor threads remain responsible for asynchronous actor work,
while the physics executor provides the common simulation timeline.

The resulting architecture deliberately retains the producer-consumer threading
model for its educational value without using thread scheduling itself as the
simulation clock.

### Producer–Consumer Coordination

Photo agencies act as task producers and drones act as task consumers through
the shared `TaskQueue`. The queue uses a bounded `LinkedBlockingQueue` whose
capacity is established during simulation setup. Photo agencies attempt to
enqueue generated or returned tasks, while available drones retrieve tasks from
the same queue.

The queue therefore provides the primary producer–consumer boundary between the
two actor groups. Queue capacity can be replaced before a simulation begins,
while task insertion, retrieval, inspection, and clearing are synchronized by
the `TaskQueue` abstraction.

### Graphical Updates

Graphical refresh is performed separately from simulation physics. A Swing
`Timer` periodically invokes the controller refresh path on the Event Dispatch
Thread. Controllers obtain current model state and presentation snapshots and
pass the resulting data to the view.

Simulation components do not update Swing components directly. When controller
operations originating outside the Event Dispatch Thread require a graphical
update, execution is transferred to Swing through the appropriate Swing
mechanism.

The separation between physics and graphical refresh allows the simulation to
advance independently of rendering frequency and prevents presentation work from
becoming part of the simulation timing model.

### Synchronization and Visibility

Shared mutable state is protected according to the component that owns it. Actor
lifecycle flags use atomic or volatile state where visibility between execution
contexts is required, while actor operations that coordinate mutable task or
diagnostic state use synchronization where necessary.

The shared task queue provides thread-safe queue semantics through
`LinkedBlockingQueue` together with synchronized access through the `TaskQueue`
abstraction. The simulation event log similarly synchronizes event insertion and
retrieval because events may originate from multiple simulation execution
contexts.

Immutable snapshots and view-data objects provide the primary boundary between
mutable simulation state and graphical presentation, allowing the view to
consume stable read-only data rather than directly accessing actor state.

### Thread Shutdown

Thread and executor termination follow explicit ownership boundaries. Actors
receive cooperative stop requests through their lifecycle flags, while the model
owns shutdown and termination of the actor executor pools.
`SimulationController` owns cancellation and termination of the physics
executor.

Potentially blocking executor termination waits are not performed on the Swing
Event Dispatch Thread. Shutdown coordination uses background execution and
returns completion handling to the Event Dispatch Thread after termination has
been confirmed.

The detailed ordering of startup, pause, resume, stop, reset, rollback, and
final application shutdown is described separately in the
`Application Lifecycle` architecture section.

### Important Invariants

Swing presentation work executes on the Event Dispatch Thread, while simulation
physics executes on its dedicated scheduled executor. Graphical refresh
frequency does not determine simulation progression, and actor execution time
does not define simulation time.

The centralized physics update controls time-dependent domain progression, while
actor threads perform work delegated to their individual work queues. Photo
agencies and drones exchange tasks through the shared bounded task queue rather
than by directly coordinating their execution.

Background executor ownership remains explicit. Actor executors belong to the
model, the physics executor belongs to `SimulationController`, and blocking
termination waits are kept outside the Event Dispatch Thread.

### Related Classes

`Main`, `SimulationController`, `Model`, `PhotoAgency`, `Drone`, `TaskQueue`,
`SimulationEventLog`, `ViewRefreshController`

### Related Decisions

The rationale for explicit thread ownership and shutdown coordination is
documented in `ADR 009`. Simulation progression and its independence from
wall-clock execution are documented in `ADR 003`. The read-only presentation
boundary between simulation threads and graphical rendering is documented in
`ADR 004`, while producer and consumer lifecycle behavior is further described
in the `Application Lifecycle` architecture document.

<hr>

## Application Lifecycle

### Purpose

The application lifecycle defines the valid operational states of the simulation
and coordinates the creation, suspension, termination, and replacement of
simulation resources. Lifecycle management ensures that background execution,
shared model state, and available user operations remain consistent throughout
startup, execution, stopping, reset, and application shutdown.

### Lifecycle States

The simulation lifecycle is represented by `SimulationState`. The defined states
are `NO_MAP_LOADED`, `READY`, `RUNNING`, `PAUSED`, `STOPPING`, and `STOPPED`.

`NO_MAP_LOADED` represents an application without an active map. `READY`
represents a prepared application in which a map is available and simulation
settings may be configured. `RUNNING` represents an active simulation, while
`PAUSED` preserves the active simulation and its resources without advancing
simulation behavior.

`STOPPING` is a transitional state used while background executors are being
terminated. Operations that could replace or reset simulation state remain
unavailable during this period. `STOPPED` is published only after simulation
executors have terminated successfully and represents a completed simulation
whose results remain available for inspection or export.

### Simulation Preparation

Loading a map establishes the conditions required for a simulation and moves the
application into the `READY` state. Simulation-specific model data, presentation
state, event-log tracking, and current selection are cleared while the
successfully loaded map remains available.

Creating a new simulation from `READY` or `STOPPED` similarly clears state
belonging to the previous run while retaining the active map. Model reset is
permitted only when actor executor pools are absent or fully terminated,
preventing background work from an earlier run from accessing newly initialized
simulation state.

### Startup

Simulation startup is treated as one lifecycle operation. The controller first
resets run-specific state and applies the current simulation configuration.
Photo-agency and drone actor pools are then created, followed by the physics
executor and periodic GUI refresh timer.

The `RUNNING` state is published only after all required actors and timers have
been created successfully. This prevents the user interface and other controller
logic from observing a running simulation whose runtime resources are only
partially initialized.

A successful startup records the simulation-start event, refreshes presentation
state, and updates the available application controls.

### Startup Rollback

If a runtime failure occurs after startup has begun, the partially initialized
simulation is rolled back rather than leaving the application in an intermediate
configuration.

Sources of additional simulation work are stopped, actors receive stop requests,
and the lifecycle enters `STOPPING`. Executor termination is then confirmed
asynchronously. Shared model state is reset only after the partially created
executors have terminated.

After successful cleanup, simulation-specific presentation and model state are
cleared and the lifecycle returns to `READY`. The loaded map and
user-configurable setup remain available so that startup may be attempted again.

### Pause and Resume

Pausing is a reversible lifecycle transition and does not destroy simulation
resources. It is permitted only from `RUNNING`.

Photo-agency and drone actors receive pause requests and the physics executor is
stopped so that simulation time and time-dependent domain behavior no longer
advance. A simulation-pause event is recorded and the graphical interface is
explicitly refreshed before periodic GUI refresh is stopped. The lifecycle then
remains in `PAUSED`.

Resuming is permitted only from `PAUSED`. Actor execution is resumed, the
lifecycle returns to `RUNNING`, a resume event is recorded, and the physics and
GUI refresh mechanisms are restarted. Existing simulation state is preserved
across the pause and resume transition.

### Stop

Stopping is a terminal operation for the current simulation run. It is permitted
from `RUNNING` or `PAUSED`.

The controller first stops the physics and graphical refresh mechanisms so that
no additional periodic simulation work is scheduled. Actors then receive
cooperative stop requests, and the lifecycle enters `STOPPING` while executor
termination is confirmed outside the Event Dispatch Thread.

`STOPPED` is published only after the actor and physics executors have
terminated successfully. A simulation-stop event is then recorded and the
graphical interface is explicitly refreshed because periodic GUI refresh is no
longer active.

If one or more executors fail to terminate within the configured shutdown
limits, the lifecycle is not advanced to `STOPPED`. The application reports the
failure and does not permit unsafe reset of simulation state.

### Reset and New Simulation

A stopped simulation retains its current map, archived results, and presentation
state so that completed tasks can be inspected or exported. Starting another run
therefore requires an explicit new-simulation operation rather than
automatically discarding the completed simulation.

Creating a new simulation clears run-specific model data, graphical simulation
data, event-log presentation state, and selection state before returning the
lifecycle to `READY`. Because reset is permitted only after actor executors have
terminated, resources from the previous run cannot continue operating against
the new simulation state.

### Control-State Coordination

`ControlStateController` maps lifecycle state to the operations available
through the graphical user interface. Map loading, map-scale changes, simulation
configuration, lifecycle commands, archive viewing, and image export are enabled
only in states where those operations are valid.

During `STOPPING`, simulation, map, and setup controls remain disabled because
executor termination has not yet been confirmed. In `STOPPED`, completed results
may be inspected or exported, but another map cannot be loaded until the user
explicitly creates a new simulation and returns the application to `READY`.

This makes lifecycle state an operational constraint rather than only a
presentation value.

### Application Shutdown

Final application shutdown follows the same resource-ownership rules as
simulation stopping. Controller-managed graphical activity and active export
work are stopped before simulation resources are terminated. The simulation
controller stops periodic timers, requests actor termination, and confirms
executor shutdown asynchronously.

The root controller guards application shutdown against repeated requests. The
application window is disposed only after successful termination of the
simulation resources, preventing normal application exit from bypassing the
defined simulation cleanup path.

### Important Invariants

Lifecycle transitions are permitted only from their defined source states.
`RUNNING` is not published until simulation runtime resources have been created
successfully, and `STOPPED` is not published until executor termination has been
confirmed.

Shared simulation state is not reset while actor executor pools remain active or
are still terminating. `STOPPING` prevents operations that could replace or
reconfigure state while cleanup is incomplete.

Pausing preserves the current simulation and its actor infrastructure, while
stopping terminates the current run. A stopped simulation retains its results
until an explicit new-simulation operation clears them.

Potentially blocking shutdown waits remain outside the Event Dispatch Thread,
and final application shutdown follows the same ownership and termination
guarantees as ordinary simulation shutdown.

### Related Classes

`SimulationState`, `SimulationController`, `ControlStateController`,
`Controller`, `Model`, `PhotoAgency`, `Drone`

### Related Decisions

The rationale for explicit resource ownership, termination guarantees, and
lifecycle coordination is documented in `ADR 009`. The runtime execution model
is described in the `Concurrency and Threading` architecture document.
Simulation-time behavior during execution and pause is governed by `ADR 003`,
while lifecycle failure propagation and user-facing error reporting relate to
`ADR 007`.

<hr>

## Map and Coordinate System

### Purpose

The map subsystem provides the spatial foundation of the simulation. It
represents the loaded raster map, defines the relationship between image pixels
and physical distance, maintains descriptive and geographic metadata, and
provides conversions between the coordinate spaces used by the simulation and
graphical presentation.

Simulation-domain positions are represented independently of the resolution at
which the map is displayed. This allows drone movement, task positions,
distances, speeds, and camera dimensions to retain consistent physical meaning
while the same map data can be rendered at a separately scaled display size.

### Map Representation

`MapModel` owns the active map and its spatial representation. A loaded raster
image is processed into distinct image representations serving different
purposes within the application.

The original image represents the complete raster resource supplied by the user.
A cropped world image removes the margins required for camera operations and
defines the usable simulation area. A separately resampled display image
represents that world image at the dimensions required by the graphical map
component.

The world image therefore provides the pixel coordinate system associated with
the simulation area, while the display image provides the pixel coordinate
system used by graphical presentation. These coordinate spaces are related but
are not treated as interchangeable.

### Coordinate Spaces

The application distinguishes between world-meter coordinates, world-image pixel
coordinates, and display pixel coordinates.

World-meter coordinates represent positions and distances within the simulation
domain. Drone bases, drone positions, task targets, movement distances, and
other spatial domain values use this coordinate system. The origin corresponds
to the upper-left corner of the cropped world image, with the horizontal axis
increasing to the right and the vertical axis increasing downward.

World-image pixel coordinates identify positions within the cropped raster
image. They provide the connection between physical simulation coordinates and
the underlying map image used for image extraction and other map-based
operations.

Display pixel coordinates identify positions within the resampled image shown by
the graphical user interface. They are presentation coordinates and may differ
from world-image pixels because the world image can be resampled to another
display size.

Keeping these spaces explicit prevents simulation behavior from depending on
graphical display resolution.

### Map Scale

The physical scale of the active map is expressed as meters per world-image
pixel. The scale therefore defines the conversion between the raster coordinate
system of the cropped world image and the physical coordinate system used by the
simulation.

A world-image position can be converted to world meters by applying the active
meters-per-pixel value. Conversion in the opposite direction maps a physical
simulation position back to the corresponding position in the world image.

Display conversion is handled independently through the relationship between the
dimensions of the world image and the dimensions of the display image.
Converting a simulation position for graphical presentation therefore involves
mapping its physical world position to the corresponding display position rather
than treating simulation coordinates directly as Swing coordinates.

The active scale belongs to the map rather than to individual drones, tasks, or
controllers. Spatial domain components can consequently operate in physical
units without maintaining their own knowledge of raster resolution.

### Map Metadata

Runtime information about the active map is represented by `MapMetadata`.
Metadata combines properties derived from the processed raster image with
optional descriptive, scale, and geographic information associated with the map.

Image-derived information includes the dimensions of the original image and the
cropped world image. Other metadata may describe the map title, source, license,
attribution, physical scale, coordinate reference system, and geographic
location of the upper-left map position.

Optional JSON sidecar metadata is loaded by `MapMetadataLoader` into
`MapFileMetadata`. This file-oriented representation is kept separate from the
runtime metadata because the external file contains only information supplied
with the map, while the runtime representation also contains properties derived
from image processing.

`MapModel` combines the available file metadata with image-derived information
when constructing the active runtime metadata.

### Metadata Sources and Overrides

Map scale and geographic reference information retain information about their
origin. A scale may originate from the application's configured default, from
the map's metadata file, or from a manual user override.

The configured default allows a raster map without sidecar metadata to remain
usable in the simulation. File metadata can replace that default when a scale is
provided with the map, while a manual scale change can subsequently override the
active value through the application.

Geographic reference information is optional and is handled independently of the
simulation's physical coordinate system. A map can therefore provide latitude,
longitude, and coordinate-reference information without making geographic
coordinates a requirement for drone movement or task positioning.

This distinction allows the simulation to operate entirely in local metric
coordinates while still retaining geographic information when it is available.

### Safe Simulation Area

The usable simulation world is derived from the loaded raster image by removing
the margins required for camera operations. The resulting cropped world image
defines the area in which simulation positions can be generated safely without
requiring image captures to extend beyond the available source raster.

Random drone-base and task positions are generated within the valid world area
and are represented as world-meter coordinates. The conversion between physical
positions and world-image pixels allows image operations to locate the
corresponding source region when a drone captures task imagery.

The relationship between the original raster, cropped world image, and camera
capture regions therefore connects simulation geometry with the image-processing
subsystem while keeping their responsibilities separate.

### Map Loading Flow

Map loading is coordinated by `MapLoadController` and performed by the map
subsystem. The selected raster resource is first validated and decoded. The
source image is then processed into the image representations required by the
simulation and presentation.

Runtime metadata is initialized from the processed image properties and
configured defaults. If an associated metadata file is available, it is parsed
and its supported values are validated before being applied to the runtime
metadata.

The resulting images and metadata are prepared before the active `MapModel`
state is replaced. The newly constructed map therefore becomes observable only
after image processing, metadata loading, and validation have completed
successfully.

After a successful load, controllers update the application lifecycle and
presentation to reflect the new active map. A failed load leaves the previously
valid map state unchanged.

### Presentation Flow

Simulation positions remain expressed in world meters while they are part of the
domain model. During graphical refresh, the controller layer obtains simulation
snapshots and converts spatial information into presentation-specific map data.

Map presentation objects contain the information required by the graphical map
component in the coordinate system appropriate for rendering. The view therefore
does not need to interpret simulation distances or access mutable map-domain
objects in order to position drones, tasks, or other map elements.

Mouse interaction follows the inverse boundary. Positions originating in the
graphical map are interpreted relative to the displayed image and converted as
required before they are used as simulation-space information.

This keeps graphical coordinates at the presentation boundary while physical
coordinates remain authoritative within the simulation domain.

### Threading Considerations

The active map is configured outside the running simulation lifecycle and is not
replaced while simulation actors are operating. Map loading and scale changes
are therefore constrained by application lifecycle state rather than being
treated as concurrent modifications to an active simulation world.

During simulation execution, map geometry and scale provide stable spatial
reference information. Background simulation components operate on
world-coordinate positions, while graphical rendering consumes read-only
presentation data through the normal controller-driven refresh path.

The map subsystem therefore does not introduce an independent execution model.
Its interaction with background simulation and Swing presentation follows the
threading and lifecycle boundaries established elsewhere in the application.

### Important Invariants

Simulation-domain positions and distances are represented in world meters rather
than display pixels. The active meters-per-pixel value defines the relationship
between physical simulation coordinates and pixels in the cropped world image.

World-image pixels and display pixels remain distinct coordinate spaces. Changes
to display dimensions do not alter simulation positions, distances, or movement
behavior.

The cropped world image defines the usable simulation area, while the original
image retains the surrounding margins required for camera operations. Spatial
positions generated for simulation use the valid world area and can therefore be
mapped back to safe regions of the source image.

Runtime metadata combines image-derived information with optional external
metadata without treating the external representation as authoritative
application state. Scale and geographic values are validated before becoming
active, and their source is retained where the application distinguishes
defaults, file metadata, and manual overrides.

A new map does not replace the currently valid map until loading, image
processing, metadata parsing, and validation have completed successfully.

### Related Classes

`MapModel`, `MapMetadata`, `MapFileMetadata`, `MapMetadataLoader`,
`MapLoadController`, `Vector2D`, `MapDroneViewData`, `MapTaskViewData`,
`ImageUtils`

### Related Decisions

The rationale for representing map scale and metadata explicitly is documented
in `ADR 011`. Validation of external map resources and transactional replacement
of map state are documented in `ADR 006` and `ADR 007`. The separation between
simulation coordinates and graphical presentation relates to the snapshot and
view-data boundary documented in `ADR 004`. Image operations that consume the
map's raster representation are described in the `Image Processing`
architecture document.


<hr>

## Image Processing

### Purpose

The image-processing subsystem transforms raster map data into the image results
produced by drones. It prepares the loaded map for simulation use, extracts
camera views at simulation positions, applies camera-specific filtering, and
constructs the image sequences required by photo, video, and zoom tasks.

Image processing remains separate from simulation geometry. Domain positions are
maintained in world meters and are converted to world-image pixels only when
raster operations require access to the map image.

### Map Image Preparation

`MapModel` maintains three raster representations of the active map. The
original image preserves the loaded source resource. A centered square crop
defines the world image used by the simulation, while a separately resampled
image provides the fixed-size representation used for graphical display.

The world image retains the spatial resolution used for camera extraction and
task imagery. The display image is produced independently and is not used as the
source for drone camera captures. This prevents graphical display resolution
from reducing the spatial detail available to simulation image processing.

Map cropping and display resampling are delegated to `ImageUtils`. The current
map-processing configuration uses bicubic interpolation when producing the
display image.

### Camera Capture

Drone image acquisition begins from a simulation position expressed in world
meters. `Drone.takePhoto()` obtains the cropped world image from the model,
converts the requested world position to world-image pixels, and extracts a
camera-sized raster region centered on that position.

The configured camera resolution is 400 by 400 pixels. The extraction is
performed by `ImageUtils.cropImageSubpixel()`, allowing the center of the crop
to retain floating-point precision rather than being restricted to integer
raster coordinates. After extraction, the resulting image is passed through the
drone's `Camera` implementation before being returned to the task-processing
logic. Camera capture therefore consists of spatial extraction followed by
camera-specific image transformation.

### Subpixel Extraction

Simulation positions use double-precision coordinates and may map to fractional
world-image pixel positions. Camera extraction therefore preserves those
fractional coordinates rather than rounding the requested center position before
cropping.

`ImageUtils.cropImageSubpixel()` creates a new output image and draws the source
image through a translated affine transform. The translation positions the
requested floating-point source coordinate at the center of the camera output,
while the configured interpolation method determines how raster values are
sampled between source pixels.

This maintains consistency between continuous simulation movement and raster
capture. Integer conversion is deferred until the raster operation itself rather
than becoming part of the simulation's spatial state.

### Interpolation and Resampling

Image interpolation is explicit configuration rather than an implicit property
of the graphical environment. Both map display resampling and camera-oriented
subpixel processing currently use bicubic interpolation.

`ImageUtils.resampleImage()` creates a new image at the requested output
dimensions and redraws the source through `Graphics2D` using the selected
interpolation hint. `cropImageSubpixel()` applies the same configurable
interpolation mechanism when sampling a translated source raster.

The image-processing subsystem therefore uses the same interpolation abstraction
for both resizing and fractional-position extraction while allowing map and
camera interpolation settings to remain independently configurable.

### Camera Filtering

The `Camera` abstraction defines the image transformation applied after spatial
capture. Each camera exposes the same `applyFilter()` operation, allowing drone
task-processing behavior to remain independent of the concrete filter type.

`CameraColor` returns the captured image without modification.
`CameraGrayscale` delegates grayscale conversion to `ImageUtils`, while
`CameraNegative` delegates RGB inversion to the same utility class.

Filtering is therefore applied as a camera property rather than as a task
property. The same task-processing logic can produce different visual results
depending on the camera assembled into the executing drone.

### Photo Task Processing

A photo task produces one camera image at the task target position.

After the drone reaches the target, image generation is delegated to its worker
thread. The target position is passed to `takePhoto()`, which performs spatial
extraction and camera filtering. The resulting image and capture position are
then stored in the task.

The task-processing lifecycle determines when capture occurs and when the task
may complete, while the image-processing subsystem determines how the resulting
raster is produced.

### Video Frame Processing

Video tasks generate images from multiple simulated positions while the drone
moves toward the task target.

The physics update determines when frame positions should occur according to the
configured video frame rate. Positions within each movement interval are
interpolated between the previous and current world-meter coordinates. Image
generation for those positions is then delegated to the drone worker thread.

Each frame is produced through the same camera pipeline as an ordinary photo:
world-meter position conversion, subpixel extraction from the world image, and
camera filtering. The corresponding capture position is stored together with the
frame.

The temporal sampling of video frames is therefore separate from raster
generation. Simulation logic determines the sequence of capture positions, while
the image-processing subsystem converts those positions into images.

### Zoom Frame Processing

A zoom task produces a synthetic zoom sequence from one camera capture at the
task position rather than capturing the map repeatedly at different simulated
locations.

The drone first creates a normal camera image at the target position. A sequence
of progressively smaller centered regions is then extracted from that image. The
crop size is interpolated from the configured starting scale to the configured
ending scale across the required number of frames.

Each cropped region is resampled back to the original camera dimensions. The
result is a sequence in which the visible source area decreases while the output
resolution remains constant. Both cropping and resampling use the configured
camera interpolation method.

The current configuration generates the zoom sequence from a scale of 1.0 to
0.40 at 20 frames per second over five seconds.

### Spatial Safety

Camera capture assumes that the complete configured camera region is available
around every valid simulation position. This constraint is enforced when the map
world area and random simulation positions are prepared.

`ModelSettings` defines camera-safe world margins as half the camera width and
height. `MapModel` verifies that the cropped world image is large enough to
contain these margins and generates random simulation positions only inside the
remaining safe area. This allows `Drone.takePhoto()` to request a full
camera-sized crop without adding task-specific boundary correction or variable
image dimensions.

### Threading Considerations

Potentially expensive task-result image processing is not performed directly in
the centralized physics update.

The physics path determines simulated movement, capture timing, processing
duration, and the positions associated with image acquisition. Actual image
construction for photo, video, and zoom results is submitted to the drone's
bounded worker queue and executed by the drone actor thread.

This separation prevents raster processing from blocking simulation progression
while retaining simulation control over when and where image results are
produced. If the drone work queue reaches capacity, new work is rejected rather
than allowing image-processing work to accumulate without limit.

The broader execution model is described in the `Concurrency and Threading`
architecture section.

### Important Invariants

Task imagery is extracted from the cropped world image rather than from the
resampled display image. Graphical display resolution therefore does not
determine task-result resolution or spatial source quality.

Simulation positions remain in world meters until image acquisition requires
conversion to world-image pixels. Fractional raster positions are preserved
through subpixel extraction.

Camera output dimensions remain fixed at the configured camera resolution. Valid
simulation positions are restricted to a camera-safe world area so that a
complete crop can be generated at those positions.

Camera filtering remains independent of task type. Task-processing behavior
determines what images are required, while the drone's camera determines the
visual transformation applied to each captured image.

Photo and video imagery represent captures at simulated world positions. Zoom
frames are instead generated by cropping and resampling one captured image at
the target position.

Potentially expensive task-result image processing executes through the drone
worker thread rather than the centralized physics update.

### Related Classes

`MapModel`, `Drone`, `Camera`, `CameraColor`, `CameraGrayscale`,
`CameraNegative`, `ImageUtils`, `Vector2D`, `Task`, `PhotoTask`, `VideoTask`,
`ZoomTask`, `ModelSettings`

### Related Decisions

The spatial relationship between simulation coordinates and raster coordinates
is documented in `ADR 011` and the `Map and Coordinate System` architecture
section. Simulation-time control of video sampling and task processing is
documented in `ADR 003`. Separation of task-result work from centralized physics
execution is documented in `ADR 009`. The scope and verification approach for
image-processing tests are documented in `ADR 010`.


<hr>

## Validation and Error Handling

### Purpose

Validation and error handling protect application state from invalid input and
provide controlled behavior when application operations fail. Validation
responsibilities are assigned to the boundary that owns each constraint, while
failures propagate outward until they reach a layer with sufficient context to
handle them meaningfully.

### Responsibilities

The view performs lightweight validation of user input where the constraint
belongs to presentation or input format and presents user-facing error messages
supplied by controllers. It does not interpret technical exceptions.

Controllers enforce interaction-specific preconditions and form the principal
error-handling boundary for user-triggered operations. They coordinate model
operations, catch expected application-level failures, record technical
diagnostic information where appropriate, and determine the user-facing
response.

The model remains responsible for validating domain invariants and rejects
values that would produce invalid application state. Model and support
components report failures through exceptions and do not directly invoke
graphical presentation.

Components that load external resources validate those resources before
incorporating them into active model state. Parsing, structural validation,
domain validation, state mutation, and error presentation therefore remain
separate responsibilities where appropriate.

### Validation Boundaries

Presentation-level validation is used for constraints that can be checked
directly from user input and where immediate feedback is useful. This validation
does not replace validation performed by the model.

Controller-level validation protects application workflows. Controllers
determine whether an operation is permitted in the current application state and
prevent invalid interactions from reaching the model. For example, map loading
is permitted only when no map is loaded or when the simulation is ready.

Model-level validation protects domain invariants independently of the caller.
`MapMetadata`, for example, rejects non-finite or non-positive map scales and
validates latitude and longitude ranges before modifying metadata state.

### External Resource Validation

Map loading provides the principal external-resource validation flow. `MapModel`
verifies that a selected map exists, is a readable file, can be decoded as a
supported image, and is large enough for the configured camera margins. Optional
JSON metadata is parsed separately by `MapMetadataLoader` and applied only after
its domain values have been validated.

The deserialized `MapFileMetadata` represents external data without defining the
authoritative domain constraints. Those constraints are enforced when the data
is applied to `MapMetadata`, keeping the external representation separate from
validated application state.

### Transactional State Changes

Operations that construct compound model state validate and process their inputs
before modifying the active state. During map loading, decoded images, derived
image representations, default metadata, and optional file metadata are prepared
in temporary local values. The active `MapModel` fields are replaced only after
all processing and validation have completed successfully.

This preserves the previously valid model state when loading or validation fails
and prevents partially processed resources from becoming observable application
state.

### Error Propagation

Failures are propagated to the architectural layer capable of determining the
appropriate application response. Low-level model and support components report
failures rather than presenting them directly.

Where multiple lower-level failures represent failure of one application-level
operation, they may be translated into a dedicated application-level exception.
The original exception is retained as the cause where appropriate, preserving
diagnostic information while preventing higher layers from depending on
implementation-specific exception types.

Map loading follows this pattern through `MapLoadException`. Failures
originating from file access, image decoding, metadata parsing, metadata
validation, or image processing are represented to the controller as failure of
the map-loading operation rather than requiring the controller to handle each
underlying implementation detail separately.

### Error Presentation and Logging

Controllers form the primary boundary between technical failures and user-facing
error reporting. When an expected application-level operation fails, the
responsible controller catches the failure, records technical diagnostic
information where appropriate, and supplies a concise title and message to the
view.

The view is responsible for presenting this information to the user but does not
inspect exceptions or determine recovery behavior. Technical exception details
therefore remain separate from graphical error presentation.

Direct stack-trace output is not used as an error-handling mechanism for
intentionally handled failures. Diagnostic information is recorded through the
application's technical logging mechanism, while user-facing messages describe
the failed operation without exposing unnecessary implementation details.

### Data Flow

User or external input enters through a presentation or resource-loading
boundary and is validated as it crosses application layers. Presentation
constraints are handled near the view, workflow constraints in controllers, and
domain invariants in the model. Valid data is committed to model state only
after the relevant constraints have been satisfied.

When an operation fails, the failure propagates in the opposite direction.
Low-level components report the failure, application-level exceptions provide an
abstraction over implementation-specific causes where appropriate, and
controllers determine the resulting application response. Technical information
is recorded for diagnostics, while concise error information is passed to the
view for presentation.

### Important Invariants

Model invariants are enforced by the model and do not depend on prior validation
by the graphical user interface. External resources do not replace valid
application state until loading, processing, and validation have completed
successfully. Validation may occur at multiple layers when those checks protect
different concerns, but identical rules are not duplicated without a separate
boundary requirement.

Low-level model and support components do not present graphical errors directly.
Expected failures are not silently suppressed, and application-level exception
translation preserves the underlying cause where diagnostic information is
required. Controllers determine the user-facing response to failures occurring
during application workflows, while the view remains responsible only for
presentation.

Validation and error handling remain separate concerns: validation determines
whether data or an operation is acceptable, while error propagation determines
how a detected failure reaches the boundary capable of handling it.

### Related Classes

`MapLoadController`, `MapModel`, `MapMetadata`, `MapFileMetadata`,
`MapMetadataLoader`, `MapLoadException`, `View`

### Related Decisions

The allocation of validation responsibilities is documented in `ADR 006`.
Exception propagation, technical diagnostics, and user-facing error reporting
are documented in `ADR 007`. Map metadata and scale constraints are documented
in `ADR 011`, while failure handling associated with background execution and
application shutdown is documented in `ADR 009`.

<hr>

## Event Logging and Monitoring

### Purpose

Event logging and monitoring provide observable information about simulation
behavior without requiring presentation components to inspect or reconstruct
mutable domain state.

The simulation event log records a bounded chronological history of significant
domain and lifecycle events. The diagnostic monitors provide periodically
refreshed textual views of the current state of photo agencies, drones, and
completed tasks. These mechanisms serve different purposes and are maintained
separately.

### Responsibilities

The model owns the structured simulation event history. Domain components record
events when significant operations or transitions occur, while
`SimulationEventLog` stores those events in chronological order.

`ViewRefreshController` transfers event data from the model to the graphical
user interface and separately refreshes the diagnostic monitors. The view
formats and displays the supplied information but does not create simulation
events or infer historical activity.

Diagnostic monitor text is produced from current model and actor state rather
than from the simulation event history. The monitors therefore describe the
current diagnostic condition of the simulation, while the event log describes
the sequence of significant operations that occurred.

### Simulation Events

A `SimulationEvent` is an immutable representation of one chronological
simulation event. Each event contains a monotonically increasing sequence
number, simulation timestamp, `SimulationEventType`, source name, and
human-readable message.

`SimulationEventType` defines the supported categories of lifecycle, task, and
drone events, including simulation start and stop operations, task creation and
processing stages, task abort and requeue operations, and selected drone state
transitions.

Events are created by the component responsible for the corresponding operation.
Simulation lifecycle events originate from the simulation controller, while task
and drone events originate from the relevant domain actors. Event timestamps use
the simulation time supplied to those operations.

### Event Storage

`SimulationEventLog` maintains the chronological event history within the model.
Event insertion and retrieval are synchronized because events may be produced by
different simulation threads. Each newly added event receives the next sequence
number before being appended to the history.

The model event history is bounded to 1,000 events. When this limit is exceeded,
the oldest events are removed. Resetting the simulation clears the history and
resets the event sequence numbering.

Consumers retrieve events using a sequence-number cursor rather than copying the
complete event history on every refresh. `getEventsSince()` returns a copied
list containing only events whose sequence numbers are greater than the caller's
last processed sequence number.

### Event Presentation

`ViewRefreshController` maintains the sequence number of the most recently
displayed event. During each GUI refresh it requests only newer events from the
model, updates its cursor to the newest returned sequence number, and appends
the new events to the view.

`EventLogPanel` presents events in chronological order using a monospaced text
display. Each entry includes formatted simulation time, event type, source, and
descriptive message. The graphical event history is independently bounded to 500
displayed events, allowing the model to retain a larger recent history than the
currently visible panel.

When simulation data is reset, both the model event history and the
presentation-side sequence tracking are reset so that events belonging to a
previous simulation are not mixed with the next run.

### Diagnostic Monitors

The diagnostic monitors are independent of the structured event log. They
present textual information describing the current state of photo agencies,
drones, and completed tasks rather than a chronological history of domain
events.

`ViewRefreshController` refreshes these monitors at a lower frequency than the
main graphical update cycle. The monitor refresh interval is one second because
replacing large text documents on every GUI refresh would introduce unnecessary
rendering work.

`MonitorsPanel` contains separate views for photo agencies, drones, and
completed tasks. Each monitor displays a count together with formatted
diagnostic text supplied by the model. `MonitorTabPanel` replaces its text only
when the supplied content differs from the currently displayed value, avoiding
unnecessary document updates.

### Data Flow

Significant simulation operations produce structured events within the model.
These events are appended to the synchronized event log and assigned sequence
numbers. During GUI refresh, the controller requests events newer than its last
processed sequence number and appends them to the event-log view.

Diagnostic monitoring follows a separate refresh path. The controller
periodically requests current formatted state from the model and replaces the
corresponding monitor text when necessary.

The event log therefore transfers incremental historical information, while the
monitors periodically transfer current diagnostic information.

### Threading Considerations

Simulation events may originate from simulation lifecycle coordination, the
centralized physics update, or actor-related operations. `SimulationEventLog`
synchronizes mutation and retrieval so that the chronological history remains
consistent when accessed across these execution contexts.

Graphical event and monitor updates are performed through the normal
controller-driven GUI refresh path. Event retrieval is incremental to reduce
repeated data transfer, while monitor refreshes are throttled independently of
the main GUI refresh frequency.

### Important Invariants

Simulation event history belongs to the model and is not constructed by the
view. Event timestamps use simulation time rather than wall-clock time. Events
are immutable after creation and receive monotonically increasing sequence
numbers within one simulation run.

The event log is bounded and does not provide permanent persistence or a
complete audit trail. Only selected operations considered significant to
simulation behavior are represented as events.

Diagnostic monitors and simulation events remain separate mechanisms. Monitors
represent current diagnostic state, while events represent chronological domain
history. Neither mechanism replaces technical application logging used for
exception diagnostics and unexpected execution failures.

### Related Classes

`SimulationEvent`, `SimulationEventType`, `SimulationEventLog`, `Model`,
`SimulationController`, `Drone`, `PhotoAgency`, `ViewRefreshController`,
`EventLogPanel`, `MonitorsPanel`, `MonitorTabPanel`

### Related Decisions

The rationale for maintaining structured simulation event history is documented
in `ADR 008`. Simulation timestamps are governed by `ADR 003`, while event
generation associated with drone state transitions relates to `ADR 005`.
Technical logging and failure diagnostics are documented separately in
`ADR 007`.

<hr>

## Testing Strategy

### Purpose

The testing strategy provides regression protection for deterministic model,
domain, and support behavior while avoiding tests whose reliability depends
primarily on Swing rendering, wall-clock timing, or thread scheduling.

Automated tests are used where application behavior can be reproduced
predictably and evaluated through stable assertions. Manual application-level
testing remains necessary for graphical presentation, interactive workflows, and
the complete concurrent simulation.

### Test Organization

The automated tests are located under `../src/test/java` and follow the package
structure of the production code. The current suite covers the central model,
components, drone behavior, simulation events, geometry, map handling, task
storage and queueing, and image-processing utilities.

Test classes are organized around production components rather than around
individual application features. Nested test classes are used where a component
contains several coherent behavioral areas, such as simulation timing,
validation, movement, metadata handling, or event-log lifecycle behavior.

Parameterized tests are used when the same behavior should be verified across
multiple implementations or representative input values without duplicating test
logic.

### Test Execution

The project uses JUnit 5 through the `junit-jupiter` dependency. Tests are
executed through Maven Surefire as part of the normal Maven test lifecycle.

The Maven build targets Java 15 and configures the compiler accordingly.
Checkstyle runs during `verify` using the repository policy under
`config/checkstyle/`. Test, Checkstyle, and Javadoc reports are also available
through the Maven reporting configuration.

The automated suite is therefore executable independently of the IDE through the
standard Maven build.

### Deterministic Model Testing

Model behavior is tested synchronously wherever possible. Simulation-time tests
advance the model explicitly through physics updates and verify the resulting
simulation duration without waiting for wall-clock time.

Drone tests similarly exercise deterministic movement, battery calculations,
position changes, and state transitions directly without starting the normal
drone worker thread. This allows domain behavior to be tested without depending
on executor scheduling.

The suite therefore separates deterministic simulation logic from the
asynchronous runtime mechanism used by the complete application.

### Validation and Failure Testing

Validation tests cover representative invalid inputs, boundary values, and
operation preconditions. These tests verify both rejection behavior and, where
important, preservation of previously valid state.

Map tests provide the clearest example. Invalid images, malformed metadata,
invalid scales, missing files, and insufficient image dimensions are rejected,
while failed replacement loads are verified not to overwrite an already valid
map configuration.

Model tests similarly verify lifecycle constraints such as rejecting simulation
reset while actor executor pools remain active.

Failure-oriented tests therefore verify not only that an exception occurs but
also that the affected subsystem remains in a valid state afterward where that
guarantee forms part of the application contract.

### Geometry and Numeric Testing

Geometry and simulation calculations use explicit tolerances for floating-point
comparisons. `Vector2D` tests cover arithmetic, distance, direction,
normalization, interpolation, movement, coordinate conversion, equality, and
validation behavior.

Parameterized inputs are used for representative numerical boundaries and
invalid values, while individual tests remain focused on one operation or
behavioral contract.

The objective is to verify mathematically significant behavior and regression
boundaries rather than exhaustively enumerate the numeric input space.

### Map and File Testing

Map-related tests use generated in-memory images and temporary filesystem
resources rather than depending on the project's demonstration maps.

JUnit temporary directories are used for sidecar metadata and file-loading
tests. Generated image data allows tests to exercise valid loading, invalid
image content, missing files, optional metadata, malformed JSON, and
transactional replacement behavior in a repeatable environment.

Random safe-position generation is tested using seeded `Random` instances where
reproducibility is useful.

### Image-Processing Testing

Image-processing tests verify representative structural and visual properties
rather than exhaustive pixel-level correctness.

Cropping and resampling tests verify dimensions and selected source-to-result
pixel relationships. Color-processing tests verify properties such as grayscale
channel equality, negative RGB inversion, alpha preservation, and output image
type.

This provides regression protection for the intended behavior without coupling
the test suite to every implementation detail of interpolation and rendering.

### Event and Collection Testing

The simulation event log is tested for sequence numbering, incremental
retrieval, defensive copying, clearing, and bounded-history behavior.

Task queue and archive tests verify storage, capacity, cleanup, and boundary
behavior. These tests focus on the contracts exposed by the corresponding model
abstractions rather than on graphical presentation of the stored data.

### Manual Verification Boundary

Automated tests do not attempt to verify Swing layout, look-and-feel rendering,
pixel-perfect graphical output, or complete user interaction sequences.

The complete concurrent simulation is also not verified through tests that rely
on real thread scheduling or timing. Instead, deterministic behavior used by the
runtime is tested directly, while application-level concurrency, responsiveness,
and visual behavior are verified by running the application.

This boundary keeps the automated suite reproducible while acknowledging that
some characteristics of a desktop simulation are better assessed through
interactive execution.

### Important Invariants

Automated tests should remain deterministic and independent of wall-clock delays
wherever possible. Tests should verify externally meaningful behavior and state
guarantees rather than internal implementation structure unless that structure
itself forms part of an architectural contract.

File-based tests should create their own temporary resources. Randomized tests
should use controlled randomness when repeatability is required. Floating-point
results should be compared using explicit tolerances appropriate to the tested
calculation.

Automated coverage is intentionally selective. The absence of GUI automation or
live scheduling tests does not imply that those areas are unimportant; they are
verified through a different testing boundary.

### Related Test Classes

`ModelTest`, `BatteryTest`, `DroneTest`, `SimulationEventLogTest`,
`Vector2DTest`, `MapMetadataLoaderTest`, `MapMetadataTest`, `MapModelTest`,
`TaskArchiveTest`, `TaskQueueTest`, `ImageUtilsTest`

### Related Decisions

The rationale for focusing automated testing on deterministic and reproducible
behavior is documented in `ADR 010`. Simulation-time determinism is documented
in `ADR 003`, validation and failure behavior in `ADR 006` and `ADR 007`, and
thread and lifecycle constraints in `ADR 009`.

<hr>
