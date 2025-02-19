/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.util.mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * Represents a bidirectional map for going between base names and aliases.
 * Allows for caseInsensitive aliases since some naming conventions are
 * ambiguous on case. The base names cannot be treated case insensitive because
 * this would cause ambiguity and require case insensitive handling of base
 * names in all namespaces.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 22, 2012            bsteffen     Initial creation
 * Apr 02, 2014 2906       bclement     changed to return empty set instead of null for lookup methods
 * Dec 15, 2015 18139      pwang        Added method mergeAliasList to enable merge alias
 * Nov 01, 2018 #7536      dgilling     Add override method.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class AliasNamespace {

    protected final boolean caseSensitive;

    /**
     * Map an alias name to base names
     */
    protected Map<String, Set<String>> alias2base = new HashMap<>();

    /**
     * maps base names to alias names.
     */
    protected Map<String, Set<String>> base2alias = new HashMap<>();

    public AliasNamespace(AliasList aliasList) {
        this.caseSensitive = aliasList.isCaseSensitive();
        int mapSize = (int) (aliasList.getAliasList().size() / 0.75) + 1;
        alias2base = new HashMap<>(mapSize, 0.75f);
        base2alias = new HashMap<>(mapSize, 0.75f);

        for (Alias def : aliasList.getAliasList()) {
            String alias = def.getAlias();
            if (!caseSensitive) {
                alias = alias.toLowerCase();
            }
            String base = def.getBase();
            Set<String> baseSet = alias2base.get(alias);
            if (baseSet == null) {
                baseSet = new HashSet<>();
                alias2base.put(alias, baseSet);
            }
            baseSet.add(base);
            Set<String> aliasSet = base2alias.get(base);
            if (aliasSet == null) {
                aliasSet = new HashSet<>();
                base2alias.put(base, aliasSet);
            }
            aliasSet.add(alias);
        }
    }


    /**
     * @param alias
     * @return empty set if no mapping from alias to base is found
     */
    public Set<String> lookupBaseNames(String alias) {
        if (!caseSensitive) {
            alias = alias.toLowerCase();
        }
        Set<String> base = alias2base.get(alias);
        if (base == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(base);
    }

    /**
     * @param base
     * @return empty set if no mapping from base to aliases is found
     */
    public Set<String> lookupAliases(String base) {
        Set<String> alias = base2alias.get(base);
        if (alias == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(alias);
    }

    /**
     * Merge Alias List into existing maps
     * @param aliasList
     */
    public void mergeAliasList(AliasList aliasList) {
        for (Alias def : aliasList.getAliasList()) {
            String alias = def.getAlias();
            if (!caseSensitive) {
                alias = alias.toLowerCase();
            }
            String base = def.getBase();
            Set<String> baseSet = alias2base.get(alias);
            if (baseSet == null) {
                baseSet = new HashSet<>();
                alias2base.put(alias, baseSet);
            }
            baseSet.add(base);
            Set<String> aliasSet = base2alias.get(base);
            if (aliasSet == null) {
                aliasSet = new HashSet<>();
                base2alias.put(base, aliasSet);
            }
            aliasSet.add(alias);
        }
    }

    /**
     * Override the mappings in this namespace with the mappings in another
     * <code>AliasNamespace</code> instance.
     *
     * @param override
     *            The namespace to use as the override. If any mappings in this
     *            namespace conflict with the existing mappings the override's
     *            mappings will take precedent.
     */
    public void override(AliasNamespace override) {
        base2alias.putAll(override.base2alias);
        alias2base.putAll(override.alias2base);
    }
}