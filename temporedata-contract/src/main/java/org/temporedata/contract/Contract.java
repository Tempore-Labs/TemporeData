package org.temporedata.contract;

/**
 * [ENT-P1] Data contract capability namespace root.
 *
 * <p>Hosts declarative data contracts (YAML/JSON: schema, SLA latency commitment, quality
 * requirements) and the Contract Validation tier that checks Backward/Full compatibility on
 * DDL change or publish. A broken contract blocks the change and notifies the upstream Data
 * Owner and downstream consumers.
 *
 * <p>Boundary contract: contract validation consumes Schema Drift from temporedata-metadata and
 * depends on no sibling module except metadata.
 */
public final class Contract {

    private Contract() {
        // namespace marker only
    }
}