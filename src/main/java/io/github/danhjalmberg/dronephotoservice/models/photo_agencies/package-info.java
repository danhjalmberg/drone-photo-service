/**
 * Implements photo agencies as task producers and result recipients.
 *
 * <p>Physics updates drive randomized production intervals, while dedicated
 * actor threads create or retry work and submit it to the shared bounded task
 * queue. Agencies archive completed results and prioritize reset aborted tasks
 * for later resubmission.</p>
 */
package io.github.danhjalmberg.dronephotoservice.models.photo_agencies;
