// AdminDepartmentHierarchy.jsx
import React, { useLayoutEffect, useEffect, useRef, useState } from 'react';
import cytoscape from 'cytoscape';
import './AdminDepartmentHierarchy.css';

const AdminDepartmentHierarchy = () => {
    const cyRef = useRef(null);
    const containerRef = useRef(null);
    const [isDirty, setIsDirty] = useState(false);
    const [mode, setMode] = useState('move');
    const [roles, setRoles] = useState([]);
    const [loadedData, setLoadedData] = useState({
        childrenMap: {},
        positionsMap: {}
    });
    const [debugData, setDebugData] = useState({ loaded: {}, toSave: {} });

    // Helper to get CSRF token from cookie
    const getCsrfTokenFromCookie = () => {
        const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return match ? decodeURIComponent(match[1]) : null;
    };

    // Fetch departments for sidebar
    useEffect(() => {
        fetch('/api/departments')
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch roles');
                return res.json();
            })
            .then(data => setRoles(data.map(d => d.name)))
            .catch(err => console.error('Error fetching roles:', err));
    }, []);

    // Reload and render saved hierarchy
    const reloadHierarchy = () => {
        fetch('/admin/hierarchy/load', {
            method: 'GET',
            credentials: 'include'
        })
            .then(res => res.json())
            .then(data => {
                const { relations = [], positions = {} } = data;
                const childrenMap = {};
                relations.forEach(({ parent, child }) => {
                    if (!childrenMap[parent]) childrenMap[parent] = [];
                    childrenMap[parent].push(child);
                });
                const positionsMap = {};
                Object.entries(positions).forEach(([role, pos]) => {
                    const x = pos.x != null ? pos.x : pos.posX;
                    const y = pos.y != null ? pos.y : pos.posY;
                    positionsMap[role] = { x, y };
                });
                setLoadedData({ childrenMap, positionsMap });
                setDebugData(prev => ({
                    ...prev,
                    loaded: { relations, positions: positionsMap }
                }));
            })
            .catch(err => console.error('Failed to load hierarchy:', err));
    };

    // Initial load
    useEffect(() => {
        reloadHierarchy();
    }, []);

    const addRole = (role) => {
        const cy = cyRef.current;
        if (!cy || cy.$id(role).length) return;
        const x = cy.width() / 2;
        const y = cy.height() / 2;
        cy.add({ group: 'nodes', data: { id: role, label: role }, position: { x, y } });
        setIsDirty(true);
    };

    const deleteHandler = (evt) => {
        cyRef.current.remove(evt.target);
        setIsDirty(true);
    };

    let srcNode = null;
    const arrowHandler = (evt) => {
        const tgt = evt.target;
        if (!srcNode) {
            srcNode = tgt;
            tgt.addClass('selected');
        } else if (tgt.id() !== srcNode.id()) {
            cyRef.current.add({
                data: { id: `${srcNode.id()}→${tgt.id()}`, source: srcNode.id(), target: tgt.id() }
            });
            srcNode.removeClass('selected');
            srcNode = null;
            setIsDirty(true);
        }
    };

    // MOVE vs ARROW – using delegated events
    const enableMove = () => {
        const cy = cyRef.current;
        if (!cy) return;
        cy.autoungrabify(false);
        cy.off('tap', 'node');
        cy.on('tap', 'node', deleteHandler);
        setMode('move');
        srcNode = null;
        cy.nodes('.selected').removeClass('selected');
    };
    const enableArrow = () => {
        const cy = cyRef.current;
        if (!cy) return;
        cy.autoungrabify(true);
        cy.off('tap', 'node');
        cy.on('tap', 'node', arrowHandler);
        setMode('arrow');
        srcNode = null;
        cy.nodes('.selected').removeClass('selected');
    };

    // 1) INIT on mount
    useEffect(() => {
        if (!containerRef.current) return;
        const style = [
            {
                selector: 'node',
                style: {
                    shape: 'round-rectangle',
                    width: 'label',
                    height: 'label',
                    padding: '8px',
                    label: 'data(label)',
                    'text-valign': 'center',
                    'background-color': '#66c',
                    color: '#fff',
                    'font-size': '12px'
                }
            },
            {
                selector: 'edge',
                style: {
                    'curve-style': 'bezier',
                    'target-arrow-shape': 'triangle',
                    'line-color': '#888',
                    'target-arrow-color': '#888'
                }
            },
            {
                selector: '.selected',
                style: {
                    'overlay-color': 'yellow',
                    'overlay-opacity': 0.3,
                    'overlay-padding': '6px'
                }
            }
        ];

        const cy = cytoscape({
            container: containerRef.current,
            elements: [],      // start empty
            style,
            layout: { name: 'preset' }
        });

        cyRef.current = cy;

        // delegated events for all nodes, now and future:
        cy.on('add remove', 'edge', () => setIsDirty(true));
        cy.on('dragfree', 'node', () => setIsDirty(true));

        // default to move mode
        enableMove();

        return () => cy.destroy();
    }, []);

    // 2) REDRAW whenever loadedData changes
    useLayoutEffect(() => {
        const { childrenMap, positionsMap } = loadedData;
        const cy = cyRef.current;
        if (!cy || Object.keys(childrenMap).length === 0) return;

        // build elements…
        const elements = [];
        Object.entries(childrenMap).forEach(([p, kids]) => {
            elements.push({
                data: { id: p, label: p },
                position: { x: positionsMap[p].x, y: positionsMap[p].y }
            });
            kids.forEach(c => {
                elements.push({
                    data: { id: c, label: c },
                    position: { x: positionsMap[c].x, y: positionsMap[c].y }
                });
                elements.push({ data: { id: `${p}→${c}`, source: p, target: c } });
            });
        });

        cy.batch(() => {
            cy.elements().remove();
            cy.add(elements);
            const layout = cy.layout({ name: 'preset' });
            layout.on('layoutstop', () => {
                cy.fit(cy.elements(), 50);
            });
            layout.run();
        });

        console.log('Cytoscape thinks it has nodes:', cyRef.current.nodes().map(n => n.id()));
        console.log('Cytoscape thinks it has edges:', cyRef.current.edges().map(e => e.id()));
        cyRef.current.nodes().forEach(n =>
            console.log(` node “${n.id()}” at`, n.position())
        );
        console.log('Graph bounding box:', cyRef.current.elements().boundingBox());
    }, [loadedData]);

    const saveHierarchy = () => {
        const seen = new Set();
        const relations = cyRef.current
            .edges()
            .map(e => ({ parent: e.data('source'), child: e.data('target') }))
            .filter(r => {
                const key = `${r.parent}→${r.child}`;
                if (seen.has(key)) return false;
                seen.add(key);
                return true;
            });
        const positions = cyRef.current
            .nodes()
            .map(n => ({ role: n.id(), x: n.position('x'), y: n.position('y') }));
        const body = { relations, positions };
        setDebugData(prev => ({ ...prev, toSave: body }));

        const headers = { 'Content-Type': 'application/json' };
        const csrf = getCsrfTokenFromCookie();
        if (csrf) headers['X-XSRF-TOKEN'] = csrf;

        fetch('/admin/hierarchy/save', {
            method: 'POST',
            headers,
            credentials: 'include',
            body: JSON.stringify(body)
        })
            .then(res => res.text().then(txt => ({ ok: res.ok, txt })))
            .then(({ ok, txt }) => {
                if (ok) {
                    alert('Saved!');
                    setIsDirty(false);
                } else {
                    alert('Save failed: ' + txt);
                }
            })
            .catch(err => {
                console.error(err);
                alert('Save error—see console');
            });
    };

    const goBack = () => {
        if (isDirty && !window.confirm('You have unsaved changes. Leave anyway?')) return;
        window.location.href = '/';
    };

    return (
        <div className="admin-hierarchy">
            <aside className="sidebar">
                <button
                    id="moveBtn"
                    className={`mode-btn ${mode === 'move' ? 'active' : ''}`}
                    onClick={enableMove}
                >
                    Move Mode
                </button>
                <button
                    id="arrowBtn"
                    className={`mode-btn ${mode === 'arrow' ? 'active' : ''}`}
                    onClick={enableArrow}
                >
                    Arrow Mode
                </button>
                <button id="saveBtn" onClick={saveHierarchy}>
                    Save Hierarchy
                </button>

                <h2>Departments</h2>
                {roles.length === 0 ? (
                    <div>Loading...</div>
                ) : (
                    roles.map((r, i) => (
                        <div key={i} className="role-item" onClick={() => addRole(r)}>
                            {r}
                        </div>
                    ))
                )}

                <hr />
                <p>
                    <strong>Instructions:</strong>
                </p>
                <ul>
                    <li>Click a department to add it.</li>
                    <li>
                        <strong>Move Mode:</strong> drag & drop; click a node to delete
                        it.
                    </li>
                    <li>
                        <strong>Arrow Mode:</strong> click source, then click target to
                        connect.
                    </li>
                    <li>
                        When finished, click “Save Hierarchy” to permanently save
                        positions and links.
                    </li>
                </ul>
            </aside>
            <div className="main-content" ref={containerRef}></div>
        </div>
    );
};

export default AdminDepartmentHierarchy;
