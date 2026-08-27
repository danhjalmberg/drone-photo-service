/**
 * Defines read-oriented data transferred from the simulation model to
 * controllers, views, and export workflows.
 *
 * <p>These objects avoid exposing drones, agencies, tasks, and drone components
 * directly. Their immutability depth varies: scalar values, immutable vectors,
 * and component-value snapshots are stable, while some snapshots intentionally
 * share collection or image references. Each type documents its own ownership
 * boundary.</p>
 */
package io.github.danhjalmberg.dronephotoservice.models.snapshots;
