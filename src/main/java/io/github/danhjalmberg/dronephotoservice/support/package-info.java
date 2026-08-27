/**
 * Provides reusable image processing, GIF encoding, task-result export, and
 * simulation-time formatting.
 *
 * <p>These stateless utilities contain neither application workflow nor Swing
 * widget state. Image operations define pixel-format and ownership boundaries;
 * exporters perform blocking filesystem work and support cooperative
 * interruption, so callers choose the appropriate worker thread.</p>
 */
package io.github.danhjalmberg.dronephotoservice.support;
