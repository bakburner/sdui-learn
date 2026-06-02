# Kitchen Sink — Complete SDUI Response Example

> **Source**: `GET /sdui/demos` with `X-Analytics-Platform: android`
>
> **Generated**: 2026-03-24 from the running composition service (DemoScreenComposer)

This is the full server response for the kitchen-sink demo screen — a single SDUI payload that exercises the section types and atomic primitives included in this snapshot. It is the canonical reference for what a complete SDUI response looks like in production shape.

> **Snapshot caveats:** this capture predates the `VideoPlayer` section type and the `sdui:*` icon-token migration, so nav-item `icon` fields here are raw Material names (e.g. `"home"`, `"sports_basketball"`) rather than the neutral tokens (`"sdui:home"`, `"sdui:basketball"`) the live server now emits. Refresh this appendix manually — `cd server && ./gradlew bootRun`, then `curl -s -H "X-Analytics-Platform: android" http://localhost:8080/sdui/demos | python3 -m json.tool` — when you want the current wire format.


```json
{
    "id": "demos",
    "schemaVersion": "1.0",
    "analyticsId": "demos-kitchen-sink",
    "traceId": "demo-1",
    "parentUri": "nba://scoreboard",
    "defaultRefreshPolicy": {
        "type": "static"
    },
    "navigation": {
        "items": [
            {
                "id": "for-you",
                "label": "For You",
                "icon": "sdui:home",
                "targetUri": "nba://for-you",
                "selected": false
            },
            {
                "id": "games",
                "label": "Games",
                "icon": "sdui:basketball",
                "targetUri": "nba://games",
                "selected": false
            },
            {
                "id": "watch",
                "label": "Watch",
                "icon": "sdui:video",
                "targetUri": "nba://watch",
                "selected": false
            },
            {
                "id": "leaders",
                "label": "Leaders",
                "icon": "sdui:leaderboard",
                "targetUri": "nba://leaders",
                "selected": false
            },
            {
                "id": "demos",
                "label": "Kitchen",
                "icon": "sdui:grid",
                "targetUri": "nba://demos",
                "selected": true
            },
            {
                "id": "home",
                "label": "NBA.com",
                "icon": "sdui:basketball",
                "targetUri": "nba://home",
                "selected": false
            }
        ]
    },
    "sections": [
        {
            "id": "demos:app-bar",
            "type": "AtomicComposite",
            "analyticsId": "demos_app_bar",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "start",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.md",
                        "end": "token:nba.spacing.md",
                        "top": "token:nba.spacing.sm",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Button",
                            "label": "",
                            "icon": "sdui:back",
                            "variant": "text",
                            "color": "token:nba.label.primary",
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://scoreboard"
                                }
                            ],
                            "accessibility": {
                                "label": "Back",
                                "role": "button"
                            }
                        },
                        {
                            "type": "Spacer",
                            "width": "token:nba.spacing.sm"
                        },
                        {
                            "type": "Text",
                            "content": "SDUI Section Types",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label.primary",
                            "accessibility": {
                                "label": "SDUI Section Types",
                                "role": "heading",
                                "headingLevel": 1
                            }
                        }
                    ]
                }
            },
            "surface": {},
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-game-card--scoreboard---composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Game Card (scoreboard) (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "Game Card (scoreboard) (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:game-panel-scoreboard~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_game_panel_scoreboard",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "stretch",
                    "id": "demo:game-panel-scoreboard~type=AtomicComposite-root",
                    "widthMode": "fill",
                    "cornerRadius": "token:nba.radius.md",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.lg",
                        "bottom": "token:nba.spacing.lg"
                    },
                    "actions": [
                        {
                            "trigger": "onActivate",
                            "type": "navigate",
                            "targetUri": "nba://game/0022400999"
                        }
                    ],
                    "accessibility": {
                        "label": "LAL vs BOS, Q3 4:32",
                        "role": "button"
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "row",
                            "alignment": "spaceBetween",
                            "crossAlignment": "center",
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png",
                                            "width": 48,
                                            "height": 48,
                                            "fit": "contain",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "accessibility": {
                                                "label": "LAL logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "LAL",
                                            "variant": "titleMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        },
                                        {
                                            "type": "Text",
                                            "content": "89",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "awayTeam.score"
                                        }
                                    ]
                                },
                                {
                                    "type": "LiveClock",
                                    "variant": "titleMedium",
                                    "format": "m:ss",
                                    "tickDirection": "down",
                                    "bindRef": "clock",
                                    "snapshotSeconds": 0,
                                    "isRunning": false
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                                            "width": 48,
                                            "height": 48,
                                            "fit": "contain",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "accessibility": {
                                                "label": "BOS logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "BOS",
                                            "variant": "titleMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        },
                                        {
                                            "type": "Text",
                                            "content": "94",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "homeTeam.score"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "content": {
                    "gameStatusText": "Q3 4:32",
                    "gameStatus": 2,
                    "homeTeam": {
                        "score": 94,
                        "tricode": "BOS"
                    },
                    "awayTeam": {
                        "score": 89,
                        "tricode": "LAL"
                    },
                    "clock": {
                        "snapshotSeconds": 272,
                        "snapshotAt": "2026-05-29T14:20:00Z",
                        "isRunning": false
                    }
                }
            },
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": {
                    "colors": [
                        "token:nba.bg.secondary",
                        "token:nba.bg.splash-screen"
                    ],
                    "direction": "diagonal"
                },
                "cornerRadius": "token:nba.radius.md",
                "shadow": {
                    "color": "#00000014",
                    "radius": 6,
                    "offsetX": 0,
                    "offsetY": 2
                }
            },
            "contentSourceId": "demo:game-panel-scoreboard",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-adslot--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "AdSlot (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "AdSlot (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "ads:gam-demo-top~type=AdSlot",
            "type": "AdSlot",
            "contentSourceId": "ads:gam-demo-top",
            "analyticsId": "demo_ad_slot",
            "refreshPolicy": {
                "type": "static"
            },
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "padding": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.md",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.md"
                },
                "background": "token:nba.bg.secondary",
                "cornerRadius": 0
            },
            "data": {
                "provider": "gam",
                "adUnitPath": "/21234567/sports/nba/homepage_top",
                "sizes": [
                    [
                        320,
                        50
                    ],
                    [
                        728,
                        90
                    ]
                ],
                "targeting": {
                    "section": "homepage",
                    "content_type": "live_game"
                },
                "collapseOnEmpty": true,
                "label": "Advertisement",
                "placeholder": {
                    "backgroundColor": "token:nba.bg.tertiary",
                    "text": "Advertisement"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-statline--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "StatLine (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "StatLine (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:stat-line~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_stat_line",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.md",
                        "end": "token:nba.spacing.md",
                        "top": "token:nba.spacing.sm",
                        "bottom": "token:nba.spacing.sm"
                    },
                    "gap": "token:nba.spacing.xs",
                    "children": [
                        {
                            "type": "Text",
                            "content": "Top Performers",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": 0,
                                "end": 0,
                                "top": 0,
                                "bottom": "token:nba.spacing.sm"
                            }
                        },
                        {
                            "type": "Container",
                            "direction": "row",
                            "crossAlignment": "center",
                            "widthMode": "fill",
                            "gap": "token:nba.spacing.sm",
                            "children": [
                                {
                                    "type": "Image",
                                    "src": "/sdui-demo/headshot.png?v=hs1628369",
                                    "width": 32,
                                    "height": 32,
                                    "fit": "cover",
                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                    "cornerRadius": "token:nba.radius.full",
                                    "accessibility": {
                                        "label": "Jayson Tatum headshot",
                                        "role": "image"
                                    }
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "flex": 1,
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "Jayson Tatum",
                                            "variant": "bodyMedium",
                                            "weight": "medium"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "BOS",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "gap": "token:nba.spacing.xs",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "PTS",
                                            "variant": "bodySmall",
                                            "color": "token:nba.label.secondary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "32",
                                            "variant": "titleSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label.accent.live"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Container",
                            "direction": "row",
                            "crossAlignment": "center",
                            "widthMode": "fill",
                            "gap": "token:nba.spacing.sm",
                            "children": [
                                {
                                    "type": "Image",
                                    "src": "/sdui-demo/headshot.png?v=hs203507",
                                    "width": 32,
                                    "height": 32,
                                    "fit": "cover",
                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                    "cornerRadius": "token:nba.radius.full",
                                    "accessibility": {
                                        "label": "LeBron James headshot",
                                        "role": "image"
                                    }
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "flex": 1,
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "LeBron James",
                                            "variant": "bodyMedium",
                                            "weight": "medium"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "LAL",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "gap": "token:nba.spacing.xs",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "PTS",
                                            "variant": "bodySmall",
                                            "color": "token:nba.label.secondary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "28",
                                            "variant": "titleSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label.accent.live"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Container",
                            "direction": "row",
                            "crossAlignment": "center",
                            "widthMode": "fill",
                            "gap": "token:nba.spacing.sm",
                            "children": [
                                {
                                    "type": "Image",
                                    "src": "/sdui-demo/headshot.png?v=hs203076",
                                    "width": 32,
                                    "height": 32,
                                    "fit": "cover",
                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                    "cornerRadius": "token:nba.radius.full",
                                    "accessibility": {
                                        "label": "Anthony Davis headshot",
                                        "role": "image"
                                    }
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "flex": 1,
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "Anthony Davis",
                                            "variant": "bodyMedium",
                                            "weight": "medium"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "LAL",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "gap": "token:nba.spacing.xs",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "REB",
                                            "variant": "bodySmall",
                                            "color": "token:nba.label.secondary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "14",
                                            "variant": "titleSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label.accent.live"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:stat-line",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-promobanner--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "PromoBanner (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "PromoBanner (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:promo-banner~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_promo_banner",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "children": [
                        {
                            "type": "Container",
                            "direction": "column",
                            "crossAlignment": "start",
                            "flex": 1.0,
                            "children": [
                                {
                                    "type": "Text",
                                    "content": "WELCOME TO SDUI",
                                    "variant": "labelSmall",
                                    "weight": "bold",
                                    "color": "token:nba.label.accent.live"
                                },
                                {
                                    "type": "Spacer",
                                    "height": "token:nba.spacing.sm"
                                },
                                {
                                    "type": "Text",
                                    "content": "All 20 semantic section types rendered from a single server response.",
                                    "variant": "bodySmall",
                                    "color": "token:nba.label.secondary",
                                    "maxLines": 2
                                },
                                {
                                    "type": "Spacer",
                                    "height": "token:nba.spacing.md"
                                },
                                {
                                    "type": "Button",
                                    "label": "Learn More",
                                    "variant": "primary",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://scoreboard"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:promo-banner",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": "token:nba.bg.secondary",
                "cornerRadius": "token:nba.radius.md",
                "shadow": {
                    "color": "#00000014",
                    "radius": 6,
                    "offsetX": 0,
                    "offsetY": 2
                },
                "padding": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-heropanel--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "HeroPanel (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "HeroPanel (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:hero-panel~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_content_card",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.sm",
                        "bottom": "token:nba.spacing.sm"
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "column",
                            "variant": "hero",
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://article/celtics-lead-series"
                                }
                            ],
                            "accessibility": {
                                "label": "Celtics Lead Series 3-1",
                                "role": "button"
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/card-wide.png?v=kwceltics",
                                            "fit": "cover",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "aspectRatio": 1.7777777777777777,
                                            "widthMode": "fill",
                                            "cornerRadii": {
                                                "topStart": "token:nba.radius.lg",
                                                "topEnd": "token:nba.radius.lg",
                                                "bottomStart": 0,
                                                "bottomEnd": 0
                                            },
                                            "accessibility": {
                                                "label": "Celtics Lead Series 3-1",
                                                "role": "image"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.md",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "ARTICLE",
                                            "variant": "labelSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 0,
                                                "bottom": "token:nba.spacing.xs"
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Celtics Lead Series 3-1",
                                            "variant": "titleSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Boston takes commanding lead after Game 4 victory",
                                            "variant": "bodySmall",
                                            "color": "token:nba.label.secondary",
                                            "maxLines": 2,
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": "token:nba.spacing.xs",
                                                "bottom": 0
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:hero-panel",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-contentrail--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "ContentRail (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "ContentRail (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:content-rail~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_content_rail",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "widthMode": "fill",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Featured Content",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.md",
                                "bottom": "token:nba.spacing.md"
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": "token:nba.spacing.md",
                            "showIndicators": false,
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-1",
                                    "width": 200,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-1"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Top 10 Plays",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "widthMode": "fill",
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 0,
                                                "bottom": 0
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwhighlights",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Top 10 Plays",
                                                        "role": "image"
                                                    },
                                                    "badge": {
                                                        "element": {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "background": "token:nba.label.accent.live",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "LIVE",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        },
                                                        "alignment": "bottomEnd"
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Top 10 Plays",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2,
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.md"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-2",
                                    "width": 200,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-2"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Player Spotlight",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "widthMode": "fill",
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 0,
                                                "bottom": 0
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwplayer",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Player Spotlight",
                                                        "role": "image"
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Player Spotlight",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2,
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.md"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-3",
                                    "width": 200,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-3"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Draft Preview",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "widthMode": "fill",
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 0,
                                                "bottom": 0
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwdraft",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Draft Preview",
                                                        "role": "image"
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Draft Preview",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2,
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.md"
                                            }
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-4",
                                    "width": 200,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-4"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Playoff Bracket",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "widthMode": "fill",
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 0,
                                                "bottom": 0
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwplayoffs",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Playoff Bracket",
                                                        "role": "image"
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Playoff Bracket",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2,
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.md"
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:content-rail",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-game-card--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Game Card (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "Game Card (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:game-panel~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_game_card",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "stretch",
                    "id": "demo:game-panel~type=AtomicComposite-root",
                    "widthMode": "fill",
                    "cornerRadius": "token:nba.radius.md",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.lg",
                        "bottom": "token:nba.spacing.lg"
                    },
                    "actions": [
                        {
                            "trigger": "onActivate",
                            "type": "navigate",
                            "targetUri": "nba://game/0022400888"
                        }
                    ],
                    "accessibility": {
                        "label": "HOU vs GSW, Final",
                        "role": "button"
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "row",
                            "alignment": "spaceBetween",
                            "crossAlignment": "center",
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612745/primary/D/512x512/logo.png",
                                            "width": 48,
                                            "height": 48,
                                            "fit": "contain",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "accessibility": {
                                                "label": "HOU logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "HOU",
                                            "variant": "titleMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        },
                                        {
                                            "type": "Text",
                                            "content": "105",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "awayTeam.score"
                                        }
                                    ]
                                },
                                {
                                    "type": "Text",
                                    "content": "Final",
                                    "variant": "labelSmall",
                                    "weight": "regular",
                                    "color": "token:nba.label.primary",
                                    "maxLines": 1,
                                    "opacity": 0.7,
                                    "bindRef": "gameStatusText"
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612744/primary/D/512x512/logo.png",
                                            "width": 48,
                                            "height": 48,
                                            "fit": "contain",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "accessibility": {
                                                "label": "GSW logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "GSW",
                                            "variant": "titleMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        },
                                        {
                                            "type": "Text",
                                            "content": "112",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "homeTeam.score"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "content": {
                    "gameStatusText": "Final",
                    "gameStatus": 3,
                    "homeTeam": {
                        "score": 112,
                        "tricode": "GSW"
                    },
                    "awayTeam": {
                        "score": 105,
                        "tricode": "HOU"
                    }
                }
            },
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": {
                    "colors": [
                        "token:nba.bg.secondary",
                        "token:nba.bg.splash-screen"
                    ],
                    "direction": "diagonal"
                },
                "cornerRadius": "token:nba.radius.md",
                "shadow": {
                    "color": "#00000014",
                    "radius": 6,
                    "offsetX": 0,
                    "offsetY": 2
                }
            },
            "contentSourceId": "demo:game-panel",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-responsive-row--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Responsive Row (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "Responsive Row (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:responsive-row~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_row",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "gap": "token:nba.spacing.lg",
                    "breakpoint": 600,
                    "widthMode": "fill",
                    "id": "demo-row-container",
                    "children": [
                        {
                            "type": "SectionSlot",
                            "id": "row-left",
                            "section": {
                                "id": "row-scoring-leader",
                                "type": "AtomicComposite",
                                "refreshPolicy": {
                                    "type": "static"
                                },
                                "data": {
                                    "ui": {
                                        "type": "Container",
                                        "direction": "column",
                                        "widthMode": "fill",
                                        "padding": {
                                            "start": "token:nba.spacing.md",
                                            "end": "token:nba.spacing.md",
                                            "top": "token:nba.spacing.sm",
                                            "bottom": "token:nba.spacing.sm"
                                        },
                                        "gap": "token:nba.spacing.xs",
                                        "children": [
                                            {
                                                "type": "Text",
                                                "content": "Scoring Leader",
                                                "variant": "titleMedium",
                                                "weight": "bold",
                                                "padding": {
                                                    "start": 0,
                                                    "end": 0,
                                                    "top": 0,
                                                    "bottom": "token:nba.spacing.sm"
                                                }
                                            },
                                            {
                                                "type": "Container",
                                                "direction": "row",
                                                "crossAlignment": "center",
                                                "widthMode": "fill",
                                                "gap": "token:nba.spacing.sm",
                                                "children": [
                                                    {
                                                        "type": "Image",
                                                        "src": "/sdui-demo/headshot.png?v=hs203999",
                                                        "width": 32,
                                                        "height": 32,
                                                        "fit": "cover",
                                                        "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                        "cornerRadius": "token:nba.radius.full",
                                                        "accessibility": {
                                                            "label": "Nikola Jokić headshot",
                                                            "role": "image"
                                                        }
                                                    },
                                                    {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "flex": 1,
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "Nikola Jokić",
                                                                "variant": "bodyMedium",
                                                                "weight": "medium"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "DEN",
                                                                "variant": "labelSmall",
                                                                "color": "token:nba.label.secondary"
                                                            }
                                                        ]
                                                    },
                                                    {
                                                        "type": "Container",
                                                        "direction": "row",
                                                        "crossAlignment": "center",
                                                        "gap": "token:nba.spacing.xs",
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "PTS",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label.secondary"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "26.4",
                                                                "variant": "titleSmall",
                                                                "weight": "bold",
                                                                "color": "token:nba.label.accent.live"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                }
                            },
                            "flex": 1.0
                        },
                        {
                            "type": "SectionSlot",
                            "id": "row-right",
                            "section": {
                                "id": "row-assists-leader",
                                "type": "AtomicComposite",
                                "refreshPolicy": {
                                    "type": "static"
                                },
                                "data": {
                                    "ui": {
                                        "type": "Container",
                                        "direction": "column",
                                        "widthMode": "fill",
                                        "padding": {
                                            "start": "token:nba.spacing.md",
                                            "end": "token:nba.spacing.md",
                                            "top": "token:nba.spacing.sm",
                                            "bottom": "token:nba.spacing.sm"
                                        },
                                        "gap": "token:nba.spacing.xs",
                                        "children": [
                                            {
                                                "type": "Text",
                                                "content": "Assists Leader",
                                                "variant": "titleMedium",
                                                "weight": "bold",
                                                "padding": {
                                                    "start": 0,
                                                    "end": 0,
                                                    "top": 0,
                                                    "bottom": "token:nba.spacing.sm"
                                                }
                                            },
                                            {
                                                "type": "Container",
                                                "direction": "row",
                                                "crossAlignment": "center",
                                                "widthMode": "fill",
                                                "gap": "token:nba.spacing.sm",
                                                "children": [
                                                    {
                                                        "type": "Image",
                                                        "src": "/sdui-demo/headshot.png?v=hs201566",
                                                        "width": 32,
                                                        "height": 32,
                                                        "fit": "cover",
                                                        "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                        "cornerRadius": "token:nba.radius.full",
                                                        "accessibility": {
                                                            "label": "Trae Young headshot",
                                                            "role": "image"
                                                        }
                                                    },
                                                    {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "flex": 1,
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "Trae Young",
                                                                "variant": "bodyMedium",
                                                                "weight": "medium"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "ATL",
                                                                "variant": "labelSmall",
                                                                "color": "token:nba.label.secondary"
                                                            }
                                                        ]
                                                    },
                                                    {
                                                        "type": "Container",
                                                        "direction": "row",
                                                        "crossAlignment": "center",
                                                        "gap": "token:nba.spacing.xs",
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "AST",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label.secondary"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "11.1",
                                                                "variant": "titleSmall",
                                                                "weight": "bold",
                                                                "color": "token:nba.label.accent.live"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                }
                            },
                            "flex": 1.0
                        }
                    ]
                }
            },
            "contentSourceId": "demo:responsive-row",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-tabgroup--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "TabGroup (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "TabGroup (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:tab-group~type=TabGroup",
            "type": "TabGroup",
            "contentSourceId": "demo:tab-group",
            "analyticsId": "demo_tab_group",
            "data": {
                "stateKey": "demo_active_tab",
                "defaultTab": "overview",
                "tabs": [
                    {
                        "id": "tab-overview",
                        "label": "Overview",
                        "stateKey": "demo_active_tab",
                        "stateValue": "overview"
                    },
                    {
                        "id": "tab-stats",
                        "label": "Stats",
                        "stateKey": "demo_active_tab",
                        "stateValue": "stats"
                    }
                ],
                "tabContents": {
                    "overview": [
                        {
                            "id": "tab-overview-card",
                            "type": "AtomicComposite",
                            "refreshPolicy": {
                                "type": "static"
                            },
                            "data": {
                                "ui": {
                                    "type": "Container",
                                    "direction": "column",
                                    "padding": {
                                        "start": "token:nba.spacing.lg",
                                        "end": "token:nba.spacing.lg",
                                        "top": "token:nba.spacing.sm",
                                        "bottom": "token:nba.spacing.sm"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "variant": "hero",
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "children": [
                                                        {
                                                            "type": "Image",
                                                            "src": "/sdui-demo/card-wide.png?v=kwseason",
                                                            "fit": "cover",
                                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                            "aspectRatio": 1.7777777777777777,
                                                            "widthMode": "fill",
                                                            "cornerRadii": {
                                                                "topStart": "token:nba.radius.lg",
                                                                "topEnd": "token:nba.radius.lg",
                                                                "bottomStart": 0,
                                                                "bottomEnd": 0
                                                            },
                                                            "accessibility": {
                                                                "label": "Season Overview",
                                                                "role": "image"
                                                            }
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": "token:nba.spacing.md",
                                                        "end": "token:nba.spacing.md",
                                                        "top": "token:nba.spacing.md",
                                                        "bottom": "token:nba.spacing.md"
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "ARTICLE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "token:nba.label.accent.live",
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": "token:nba.spacing.xs"
                                                            }
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "Season Overview",
                                                            "variant": "titleSmall",
                                                            "weight": "bold",
                                                            "color": "token:nba.label.primary",
                                                            "maxLines": 2
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "The 2024-25 season has been full of surprises",
                                                            "variant": "bodySmall",
                                                            "color": "token:nba.label.secondary",
                                                            "maxLines": 2,
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": 0
                                                            }
                                                        }
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                    ],
                    "stats": [
                        {
                            "id": "tab-stats-card",
                            "type": "AtomicComposite",
                            "refreshPolicy": {
                                "type": "static"
                            },
                            "data": {
                                "ui": {
                                    "type": "Container",
                                    "direction": "column",
                                    "padding": {
                                        "start": "token:nba.spacing.lg",
                                        "end": "token:nba.spacing.lg",
                                        "top": "token:nba.spacing.sm",
                                        "bottom": "token:nba.spacing.sm"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "variant": "hero",
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "children": [
                                                        {
                                                            "type": "Image",
                                                            "src": "/sdui-demo/card-wide.png?v=kwstats",
                                                            "fit": "cover",
                                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                            "aspectRatio": 1.7777777777777777,
                                                            "widthMode": "fill",
                                                            "cornerRadii": {
                                                                "topStart": "token:nba.radius.lg",
                                                                "topEnd": "token:nba.radius.lg",
                                                                "bottomStart": 0,
                                                                "bottomEnd": 0
                                                            },
                                                            "accessibility": {
                                                                "label": "League Statistical Leaders",
                                                                "role": "image"
                                                            }
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": "token:nba.spacing.md",
                                                        "end": "token:nba.spacing.md",
                                                        "top": "token:nba.spacing.md",
                                                        "bottom": "token:nba.spacing.md"
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "INTERACTIVE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "token:nba.label.accent.live",
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": "token:nba.spacing.xs"
                                                            }
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "League Statistical Leaders",
                                                            "variant": "titleSmall",
                                                            "weight": "bold",
                                                            "color": "token:nba.label.primary",
                                                            "maxLines": 2
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "Points, rebounds, assists and more",
                                                            "variant": "bodySmall",
                                                            "color": "token:nba.label.secondary",
                                                            "maxLines": 2,
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": 0
                                                            }
                                                        }
                                                    ]
                                                }
                                            ]
                                        }
                                    ]
                                }
                            }
                        }
                    ]
                }
            },
            "subsections": [
                {
                    "id": "tab-overview",
                    "actions": [
                        {
                            "trigger": "onActivate",
                            "type": "mutate",
                            "target": "demo_active_tab",
                            "value": "overview"
                        }
                    ]
                },
                {
                    "id": "tab-stats",
                    "actions": [
                        {
                            "trigger": "onActivate",
                            "type": "mutate",
                            "target": "demo_active_tab",
                            "value": "stats"
                        }
                    ]
                }
            ],
            "surface": {
                "cornerRadius": 0,
                "background": "token:nba.bg.secondary",
                "padding": {
                    "top": "token:nba.spacing.sm",
                    "end": "token:nba.spacing.md",
                    "bottom": "token:nba.spacing.xs",
                    "start": "token:nba.spacing.md"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-boxscoretable--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "BoxscoreTable (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "BoxscoreTable (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:boxscore-table~type=BoxscoreTable",
            "type": "BoxscoreTable",
            "contentSourceId": "demo:boxscore-table",
            "analyticsId": "demo_boxscore_table",
            "data": {
                "teamTricode": "BOS",
                "teamName": "Boston Celtics",
                "teamColor": "#007A33",
                "teamLogoUrl": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                "columns": [
                    {
                        "key": "min",
                        "label": "MIN",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "pts",
                        "label": "PTS",
                        "sortable": true,
                        "highlighted": true
                    },
                    {
                        "key": "reb",
                        "label": "REB",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "ast",
                        "label": "AST",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "fgPct",
                        "label": "FG%",
                        "sortable": true,
                        "highlighted": false
                    }
                ],
                "players": [
                    {
                        "playerId": "1628369",
                        "name": "J. Tatum",
                        "position": "SF",
                        "jerseyNumber": "0",
                        "imageUrl": "/sdui-demo/headshot.png?v=hs1628369",
                        "starter": true,
                        "stats": {
                            "min": "38:12",
                            "pts": 32,
                            "fgPct": ".545",
                            "ast": 5,
                            "reb": 8
                        }
                    },
                    {
                        "playerId": "1627759",
                        "name": "J. Brown",
                        "position": "SG",
                        "jerseyNumber": "7",
                        "imageUrl": "/sdui-demo/headshot.png?v=hs1627759",
                        "starter": true,
                        "stats": {
                            "min": "36:45",
                            "pts": 26,
                            "fgPct": ".480",
                            "ast": 3,
                            "reb": 5
                        }
                    },
                    {
                        "playerId": "1629684",
                        "name": "D. White",
                        "position": "PG",
                        "jerseyNumber": "9",
                        "imageUrl": "/sdui-demo/headshot.png?v=hs1629684",
                        "starter": false,
                        "stats": {
                            "min": "32:10",
                            "pts": 18,
                            "fgPct": ".500",
                            "ast": 6,
                            "reb": 4
                        }
                    }
                ],
                "sortStateKey": "demo_boxscore_sortCol",
                "sortDirectionStateKey": "demo_boxscore_sortDir"
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-form--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Form (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "Form (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:form~type=Form",
            "type": "Form",
            "contentSourceId": "demo:form",
            "analyticsId": "demo_stats_filter_form",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "layout": "vertical",
                "fields": [
                    {
                        "fieldId": "season",
                        "fieldType": "select",
                        "label": "Season",
                        "stateKey": "form_season",
                        "options": [
                            {
                                "label": "2025-26",
                                "value": "2025-26"
                            },
                            {
                                "label": "2024-25",
                                "value": "2024-25"
                            },
                            {
                                "label": "2023-24",
                                "value": "2023-24"
                            },
                            {
                                "label": "2022-23",
                                "value": "2022-23"
                            }
                        ]
                    },
                    {
                        "fieldId": "seasonType",
                        "fieldType": "select",
                        "label": "Season Type",
                        "stateKey": "form_season_type",
                        "options": [
                            {
                                "label": "Regular Season",
                                "value": "regular"
                            },
                            {
                                "label": "Playoffs",
                                "value": "playoffs"
                            },
                            {
                                "label": "All-Star",
                                "value": "allstar"
                            }
                        ]
                    },
                    {
                        "fieldId": "perMode",
                        "fieldType": "select",
                        "label": "Per Mode",
                        "stateKey": "form_per_mode",
                        "options": [
                            {
                                "label": "Per Game",
                                "value": "per_game"
                            },
                            {
                                "label": "Totals",
                                "value": "totals"
                            },
                            {
                                "label": "Per 36 Min",
                                "value": "per_36"
                            }
                        ]
                    },
                    {
                        "fieldId": "statCategory",
                        "fieldType": "select",
                        "label": "Stat Category",
                        "stateKey": "form_stat_category",
                        "options": [
                            {
                                "label": "PTS",
                                "value": "pts"
                            },
                            {
                                "label": "REB",
                                "value": "reb"
                            },
                            {
                                "label": "AST",
                                "value": "ast"
                            },
                            {
                                "label": "STL",
                                "value": "stl"
                            },
                            {
                                "label": "BLK",
                                "value": "blk"
                            },
                            {
                                "label": "FG%",
                                "value": "fgPct"
                            },
                            {
                                "label": "3PM",
                                "value": "tpm"
                            },
                            {
                                "label": "FT%",
                                "value": "ftPct"
                            }
                        ]
                    }
                ],
                "submitAction": {
                    "trigger": "onSubmit",
                    "type": "refresh",
                    "target": "stats-api:leaders~type=SeasonLeadersTable",
                    "endpoint": "/v1/sdui/screen/leaders",
                    "onFailure": "halt",
                    "failureFeedback": {
                        "message": "Stats lookup failed — please try again",
                        "style": "snackbar"
                    },
                    "paramBindings": {
                        "season": "{{form_season}}",
                        "seasonType": "{{form_season_type}}",
                        "perMode": "{{form_per_mode}}",
                        "statCategory": "{{form_stat_category}}"
                    }
                },
                "submitLabel": "Search"
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-seasonleaderstable--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SeasonLeadersTable (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "SeasonLeadersTable (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "stats-api:leaders~type=SeasonLeadersTable",
            "type": "SeasonLeadersTable",
            "contentSourceId": "stats-api:leaders",
            "analyticsId": "season_leaders_table",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "title": "Season Leaders",
                "subtitle": "2025-26 Regular Season — Per Game — sorted by PTS",
                "sortColumn": "pts",
                "sortDirection": "desc",
                "totalRows": 30,
                "page": 1,
                "pageSize": 15,
                "columns": [
                    {
                        "key": "gp",
                        "label": "GP",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "min",
                        "label": "MIN",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "pts",
                        "label": "PTS",
                        "sortable": true,
                        "highlighted": true
                    },
                    {
                        "key": "fgm",
                        "label": "FGM",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "fga",
                        "label": "FGA",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "fgPct",
                        "label": "FG%",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "tpm",
                        "label": "3PM",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "tpa",
                        "label": "3PA",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "tpPct",
                        "label": "3P%",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "ftm",
                        "label": "FTM",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "fta",
                        "label": "FTA",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "ftPct",
                        "label": "FT%",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "reb",
                        "label": "REB",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "ast",
                        "label": "AST",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "stl",
                        "label": "STL",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "blk",
                        "label": "BLK",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "tov",
                        "label": "TOV",
                        "sortable": true,
                        "highlighted": false
                    },
                    {
                        "key": "eff",
                        "label": "EFF",
                        "sortable": true,
                        "highlighted": false
                    }
                ],
                "players": [
                    {
                        "rank": 1,
                        "playerId": "203999",
                        "name": "Luka Dončić",
                        "team": "LAL",
                        "stats": {
                            "gp": 49,
                            "min": 35.4,
                            "pts": 32.4,
                            "fgm": 10.3,
                            "fga": 21.8,
                            "fgPct": 47.3,
                            "tpm": 3.7,
                            "tpa": 10.2,
                            "tpPct": 35.9,
                            "ftm": 8.0,
                            "fta": 10.4,
                            "ftPct": 77.3,
                            "reb": 7.0,
                            "ast": 8.6,
                            "stl": 1.4,
                            "blk": 0.5,
                            "tov": 4.0,
                            "eff": 32.7
                        }
                    },
                    {
                        "rank": 2,
                        "playerId": "1630175",
                        "name": "Shai Gilgeous-Alexander",
                        "team": "OKC",
                        "stats": {
                            "gp": 52,
                            "min": 33.4,
                            "pts": 31.7,
                            "fgm": 10.9,
                            "fga": 19.8,
                            "fgPct": 55.1,
                            "tpm": 1.7,
                            "tpa": 4.5,
                            "tpPct": 38.4,
                            "ftm": 8.2,
                            "fta": 9.2,
                            "ftPct": 89.3,
                            "reb": 3.9,
                            "ast": 6.5,
                            "stl": 1.4,
                            "blk": 0.8,
                            "tov": 2.1,
                            "eff": 32.7
                        }
                    },
                    {
                        "rank": 3,
                        "playerId": "1630162",
                        "name": "Anthony Edwards",
                        "team": "MIN",
                        "stats": {
                            "gp": 52,
                            "min": 35.7,
                            "pts": 29.7,
                            "fgm": 10.3,
                            "fga": 20.9,
                            "fgPct": 49.4,
                            "tpm": 3.5,
                            "tpa": 8.6,
                            "tpPct": 40.2,
                            "ftm": 5.6,
                            "fta": 7.1,
                            "ftPct": 78.7,
                            "reb": 4.6,
                            "ast": 5.2,
                            "stl": 1.4,
                            "blk": 0.8,
                            "tov": 2.8,
                            "eff": 25.9
                        }
                    },
                    {
                        "rank": 4,
                        "playerId": "1630178",
                        "name": "Tyrese Maxey",
                        "team": "PHI",
                        "stats": {
                            "gp": 60,
                            "min": 38.3,
                            "pts": 28.9,
                            "fgm": 10.1,
                            "fga": 21.9,
                            "fgPct": 46.0,
                            "tpm": 3.3,
                            "tpa": 8.9,
                            "tpPct": 37.2,
                            "ftm": 5.5,
                            "fta": 6.2,
                            "ftPct": 89.2,
                            "reb": 3.9,
                            "ast": 6.7,
                            "stl": 2.0,
                            "blk": 0.8,
                            "tov": 2.4,
                            "eff": 27.7
                        }
                    },
                    {
                        "rank": 5,
                        "playerId": "1628369",
                        "name": "Jaylen Brown",
                        "team": "BOS",
                        "stats": {
                            "gp": 55,
                            "min": 34.3,
                            "pts": 28.9,
                            "fgm": 10.7,
                            "fga": 22.2,
                            "fgPct": 48.0,
                            "tpm": 2.1,
                            "tpa": 5.9,
                            "tpPct": 34.8,
                            "ftm": 5.5,
                            "fta": 7.1,
                            "ftPct": 77.9,
                            "reb": 6.1,
                            "ast": 5.0,
                            "stl": 1.0,
                            "blk": 0.4,
                            "tov": 3.6,
                            "eff": 25.8
                        }
                    },
                    {
                        "rank": 6,
                        "playerId": "203999b",
                        "name": "Nikola Jokić",
                        "team": "DEN",
                        "stats": {
                            "gp": 46,
                            "min": 34.6,
                            "pts": 28.7,
                            "fgm": 10.1,
                            "fga": 17.7,
                            "fgPct": 57.0,
                            "tpm": 1.9,
                            "tpa": 4.8,
                            "tpPct": 40.1,
                            "ftm": 6.5,
                            "fta": 7.9,
                            "ftPct": 82.7,
                            "reb": 12.6,
                            "ast": 10.3,
                            "stl": 1.3,
                            "blk": 0.8,
                            "tov": 3.7,
                            "eff": 41.1
                        }
                    },
                    {
                        "rank": 7,
                        "playerId": "1628378",
                        "name": "Donovan Mitchell",
                        "team": "CLE",
                        "stats": {
                            "gp": 55,
                            "min": 33.5,
                            "pts": 28.5,
                            "fgm": 9.9,
                            "fga": 20.6,
                            "fgPct": 48.3,
                            "tpm": 3.5,
                            "tpa": 9.4,
                            "tpPct": 36.9,
                            "ftm": 5.2,
                            "fta": 6.1,
                            "ftPct": 85.2,
                            "reb": 3.7,
                            "ast": 5.8,
                            "stl": 1.6,
                            "blk": 0.3,
                            "tov": 3.1,
                            "eff": 26.1
                        }
                    },
                    {
                        "rank": 8,
                        "playerId": "202695",
                        "name": "Kawhi Leonard",
                        "team": "LAC",
                        "stats": {
                            "gp": 47,
                            "min": 32.4,
                            "pts": 27.9,
                            "fgm": 9.7,
                            "fga": 19.6,
                            "fgPct": 49.7,
                            "tpm": 2.6,
                            "tpa": 7.0,
                            "tpPct": 37.9,
                            "ftm": 5.8,
                            "fta": 6.4,
                            "ftPct": 90.6,
                            "reb": 6.4,
                            "ast": 3.7,
                            "stl": 2.0,
                            "blk": 0.5,
                            "tov": 2.1,
                            "eff": 27.8
                        }
                    },
                    {
                        "rank": 9,
                        "playerId": "1628973",
                        "name": "Jalen Brunson",
                        "team": "NYK",
                        "stats": {
                            "gp": 58,
                            "min": 34.7,
                            "pts": 26.5,
                            "fgm": 9.4,
                            "fga": 20.1,
                            "fgPct": 46.7,
                            "tpm": 2.8,
                            "tpa": 7.5,
                            "tpPct": 37.8,
                            "ftm": 4.8,
                            "fta": 5.8,
                            "ftPct": 84.1,
                            "reb": 2.9,
                            "ast": 6.3,
                            "stl": 0.7,
                            "blk": 0.1,
                            "tov": 2.3,
                            "eff": 23.1
                        }
                    },
                    {
                        "rank": 10,
                        "playerId": "201142",
                        "name": "Kevin Durant",
                        "team": "HOU",
                        "stats": {
                            "gp": 57,
                            "min": 36.6,
                            "pts": 26.3,
                            "fgm": 9.2,
                            "fga": 18.1,
                            "fgPct": 51.0,
                            "tpm": 2.4,
                            "tpa": 5.9,
                            "tpPct": 40.1,
                            "ftm": 5.5,
                            "fta": 6.1,
                            "ftPct": 89.1,
                            "reb": 4.9,
                            "ast": 4.5,
                            "stl": 0.8,
                            "blk": 0.9,
                            "tov": 3.2,
                            "eff": 25.2
                        }
                    },
                    {
                        "rank": 11,
                        "playerId": "1628974",
                        "name": "Jamal Murray",
                        "team": "DEN",
                        "stats": {
                            "gp": 57,
                            "min": 35.2,
                            "pts": 25.7,
                            "fgm": 8.9,
                            "fga": 18.4,
                            "fgPct": 48.4,
                            "tpm": 3.2,
                            "tpa": 7.5,
                            "tpPct": 42.9,
                            "ftm": 4.6,
                            "fta": 5.2,
                            "ftPct": 87.9,
                            "reb": 4.0,
                            "ast": 7.3,
                            "stl": 1.0,
                            "blk": 0.4,
                            "tov": 2.4,
                            "eff": 26.1
                        }
                    },
                    {
                        "rank": 12,
                        "playerId": "1630595",
                        "name": "Cade Cunningham",
                        "team": "DET",
                        "stats": {
                            "gp": 54,
                            "min": 35.1,
                            "pts": 25.2,
                            "fgm": 8.9,
                            "fga": 19.5,
                            "fgPct": 45.7,
                            "tpm": 1.9,
                            "tpa": 5.9,
                            "tpPct": 32.9,
                            "ftm": 5.4,
                            "fta": 6.6,
                            "ftPct": 81.1,
                            "reb": 4.9,
                            "ast": 9.9,
                            "stl": 1.5,
                            "blk": 0.9,
                            "tov": 3.8,
                            "eff": 27.7
                        }
                    },
                    {
                        "rank": 13,
                        "playerId": "1626164",
                        "name": "Devin Booker",
                        "team": "PHX",
                        "stats": {
                            "gp": 45,
                            "min": 33.3,
                            "pts": 24.6,
                            "fgm": 8.1,
                            "fga": 17.9,
                            "fgPct": 45.1,
                            "tpm": 1.7,
                            "tpa": 5.5,
                            "tpPct": 31.3,
                            "ftm": 6.6,
                            "fta": 7.7,
                            "ftPct": 86.2,
                            "reb": 3.1,
                            "ast": 6.1,
                            "stl": 0.9,
                            "blk": 0.3,
                            "tov": 3.3,
                            "eff": 21.6
                        }
                    },
                    {
                        "rank": 14,
                        "playerId": "1631094",
                        "name": "Deni Avdija",
                        "team": "POR",
                        "stats": {
                            "gp": 48,
                            "min": 33.5,
                            "pts": 24.4,
                            "fgm": 7.5,
                            "fga": 16.1,
                            "fgPct": 46.3,
                            "tpm": 2.1,
                            "tpa": 6.2,
                            "tpPct": 34.1,
                            "ftm": 7.4,
                            "fta": 9.2,
                            "ftPct": 80.0,
                            "reb": 5.9,
                            "ast": 6.6,
                            "stl": 0.8,
                            "blk": 0.6,
                            "tov": 3.8,
                            "eff": 25.1
                        }
                    },
                    {
                        "rank": 15,
                        "playerId": "201935",
                        "name": "James Harden",
                        "team": "CLE",
                        "stats": {
                            "gp": 53,
                            "min": 35.0,
                            "pts": 24.3,
                            "fgm": 7.1,
                            "fga": 16.7,
                            "fgPct": 42.5,
                            "tpm": 3.0,
                            "tpa": 8.4,
                            "tpPct": 36.1,
                            "ftm": 7.1,
                            "fta": 8.0,
                            "ftPct": 89.3,
                            "reb": 4.9,
                            "ast": 8.1,
                            "stl": 1.2,
                            "blk": 0.4,
                            "tov": 3.7,
                            "eff": 24.8
                        }
                    }
                ],
                "emptyMessage": "No stats available for 2025-26 Regular Season"
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-game-card--featured---composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Game Card (featured) (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "Game Card (featured) (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:featured-game-panel~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_featured_game_panel",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "stretch",
                    "id": "demo:featured-game-panel~type=AtomicComposite-root",
                    "widthMode": "fill",
                    "cornerRadius": "token:nba.radius.lg",
                    "padding": {
                        "start": 20,
                        "end": 20,
                        "top": 20,
                        "bottom": 20
                    },
                    "actions": [
                        {
                            "trigger": "onActivate",
                            "type": "navigate",
                            "targetUri": "nba://game/0022400777"
                        }
                    ],
                    "accessibility": {
                        "label": "BKN vs MIA, Q4 2:15",
                        "role": "button"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "LIVE",
                            "variant": "labelSmall",
                            "weight": "bold",
                            "color": "#FFFFFF",
                            "background": "#E03131",
                            "cornerRadius": "token:nba.radius.sm",
                            "padding": {
                                "start": "token:nba.spacing.sm",
                                "end": "token:nba.spacing.sm",
                                "top": "token:nba.spacing.xs",
                                "bottom": "token:nba.spacing.xs"
                            }
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.md"
                        },
                        {
                            "type": "Container",
                            "direction": "row",
                            "alignment": "spaceBetween",
                            "crossAlignment": "center",
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612751/primary/D/512x512/logo.png",
                                            "width": 48,
                                            "height": 48,
                                            "fit": "contain",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "accessibility": {
                                                "label": "BKN logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "BKN",
                                            "variant": "titleMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        },
                                        {
                                            "type": "Text",
                                            "content": "97",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "awayTeam.score"
                                        }
                                    ]
                                },
                                {
                                    "type": "LiveClock",
                                    "variant": "titleLarge",
                                    "format": "m:ss",
                                    "tickDirection": "down",
                                    "bindRef": "clock",
                                    "snapshotSeconds": 0,
                                    "isRunning": false
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612748/primary/D/512x512/logo.png",
                                            "width": 48,
                                            "height": 48,
                                            "fit": "contain",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "accessibility": {
                                                "label": "MIA logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "MIA",
                                            "variant": "titleMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        },
                                        {
                                            "type": "Text",
                                            "content": "101",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "homeTeam.score"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "content": {
                    "gameStatusText": "Q4 2:15",
                    "gameStatus": 2,
                    "homeTeam": {
                        "score": 101,
                        "tricode": "MIA"
                    },
                    "awayTeam": {
                        "score": 97,
                        "tricode": "BKN"
                    },
                    "clock": {
                        "snapshotSeconds": 135,
                        "snapshotAt": "2026-05-29T14:20:00Z",
                        "isRunning": false
                    }
                }
            },
            "contentSourceId": "demo:featured-game-panel",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-videocarousel--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "VideoCarousel (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "VideoCarousel (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:video-carousel~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_video_carousel",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "widthMode": "fill",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Top Highlights",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label.primary",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.xs",
                                "bottom": "token:nba.spacing.xs"
                            }
                        },
                        {
                            "type": "Text",
                            "content": "Today's best plays",
                            "variant": "bodySmall",
                            "color": "token:nba.label.secondary",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.xs",
                                "bottom": "token:nba.spacing.xs"
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": "token:nba.spacing.md",
                            "showIndicators": false,
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "vid-1",
                                    "width": 240,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-1"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Dončić No-Look Dime",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwpass",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.sm",
                                                        "bottom": 0
                                                    },
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Dončić No-Look Dime",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": "token:nba.spacing.xs"
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "crossAlignment": "center",
                                                            "background": "token:nba.label.accent.live",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "NEW",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        },
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "crossAlignment": "center",
                                                            "background": "#000000B3",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "1:24",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": "token:nba.spacing.md",
                                                "end": "token:nba.spacing.md",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.sm"
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Dončić No-Look Dime",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Lakers vs Celtics",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": 0
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "vid-2",
                                    "width": 240,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-2"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "SGA Crossover & Finish",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwcrossover",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.sm",
                                                        "bottom": 0
                                                    },
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "SGA Crossover & Finish",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": "token:nba.spacing.xs"
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Spacer",
                                                            "height": "token:nba.spacing.xs"
                                                        },
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "crossAlignment": "center",
                                                            "background": "#000000B3",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "0:48",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": "token:nba.spacing.md",
                                                "end": "token:nba.spacing.md",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.sm"
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "SGA Crossover & Finish",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Thunder vs Nuggets",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": 0
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "vid-3",
                                    "width": 240,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-3"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Edwards Poster Dunk",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwdunk",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.sm",
                                                        "bottom": 0
                                                    },
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Edwards Poster Dunk",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": "token:nba.spacing.xs"
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "crossAlignment": "center",
                                                            "background": "token:nba.label.accent.live",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "NEW",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        },
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "crossAlignment": "center",
                                                            "background": "#000000B3",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "0:32",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": "token:nba.spacing.md",
                                                "end": "token:nba.spacing.md",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.sm"
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Edwards Poster Dunk",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Timberwolves vs Suns",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": 0
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "vid-4",
                                    "width": 240,
                                    "cornerRadius": 0,
                                    "background": {
                                        "colors": [
                                            "token:nba.bg.secondary",
                                            "token:nba.bg.tertiary"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-4"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Jokić Triple-Double Recap",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwtriple-double",
                                                    "variant": "thumbnail",
                                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.sm",
                                                        "bottom": 0
                                                    },
                                                    "widthMode": "fill",
                                                    "aspectRatio": 1.7777777777777777,
                                                    "accessibility": {
                                                        "label": "Jokić Triple-Double Recap",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": "token:nba.spacing.xs"
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Spacer",
                                                            "height": "token:nba.spacing.xs"
                                                        },
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "crossAlignment": "center",
                                                            "background": "#000000B3",
                                                            "cornerRadius": "token:nba.radius.sm",
                                                            "padding": {
                                                                "start": "token:nba.spacing.xs",
                                                                "end": "token:nba.spacing.xs",
                                                                "top": "token:nba.spacing.xs",
                                                                "bottom": "token:nba.spacing.xs"
                                                            },
                                                            "children": [
                                                                {
                                                                    "type": "Text",
                                                                    "content": "3:15",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label-inverted.primary"
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": "token:nba.spacing.md",
                                                "end": "token:nba.spacing.md",
                                                "top": "token:nba.spacing.sm",
                                                "bottom": "token:nba.spacing.sm"
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Jokić Triple-Double Recap",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Full highlights",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": "token:nba.spacing.xs",
                                                        "bottom": 0
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:video-carousel",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-nbatvschedule--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "NbaTvSchedule (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "NbaTvSchedule (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:nbatv-schedule~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_nbatv_schedule",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "start",
                    "widthMode": "fill",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.lg"
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "column",
                            "alignment": "end",
                            "crossAlignment": "start",
                            "widthMode": "fill",
                            "cornerRadius": 0,
                            "children": [
                                {
                                    "type": "Image",
                                    "src": "/sdui-demo/card-wide.png?v=heroarena",
                                    "height": 200,
                                    "fit": "cover",
                                    "placeholder": "/sdui-demo/placeholder-tiny.png",
                                    "widthMode": "fill",
                                    "accessibility": {
                                        "label": "NBA TV Live",
                                        "role": "image"
                                    }
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "crossAlignment": "start",
                                    "widthMode": "fill",
                                    "padding": {
                                        "start": "token:nba.spacing.lg",
                                        "end": "token:nba.spacing.lg",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.lg"
                                    },
                                    "background": {
                                        "colors": [
                                            "#00000000",
                                            "#000000CC"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "cornerRadius": "token:nba.radius.sm",
                                            "background": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": "token:nba.spacing.xs",
                                                "end": "token:nba.spacing.xs",
                                                "top": "token:nba.spacing.xs",
                                                "bottom": "token:nba.spacing.xs"
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "LIVE",
                                                    "variant": "labelSmall",
                                                    "weight": "bold",
                                                    "color": "token:nba.label-inverted.primary"
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "NBA TV Live",
                                            "variant": "titleLarge",
                                            "weight": "bold",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Lakers vs Celtics — Coverage begins at 7:00 PM ET",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.md"
                        },
                        {
                            "type": "Text",
                            "content": "Today's Schedule",
                            "variant": "titleSmall",
                            "weight": "bold",
                            "color": "token:nba.label.primary",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.xs",
                                "bottom": "token:nba.spacing.xs"
                            },
                            "accessibility": {
                                "label": "Today's Schedule",
                                "role": "heading",
                                "headingLevel": 3
                            }
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "crossAlignment": "start",
                            "widthMode": "fill",
                            "gap": "token:nba.spacing.sm",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.xs",
                                "bottom": 0
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "id": "slot-1",
                                    "widthMode": "fill",
                                    "gap": "token:nba.spacing.md",
                                    "cornerRadius": "token:nba.radius.md",
                                    "background": "token:nba.bg.primary",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.md",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "18:00",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.secondary"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "crossAlignment": "start",
                                            "widthMode": "fill",
                                            "flex": 1.0,
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "NBA GameTime",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Pre-game analysis and predictions",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "id": "slot-2",
                                    "widthMode": "fill",
                                    "gap": "token:nba.spacing.md",
                                    "cornerRadius": "token:nba.radius.md",
                                    "background": "token:nba.bg.primary",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.md",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://game/0022400999"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "19:30",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.secondary"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "crossAlignment": "start",
                                            "widthMode": "fill",
                                            "flex": 1.0,
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "LAL @ BOS",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Live game broadcast",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "cornerRadius": "token:nba.radius.sm",
                                            "background": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": "token:nba.spacing.xs",
                                                "end": "token:nba.spacing.xs",
                                                "top": "token:nba.spacing.xs",
                                                "bottom": "token:nba.spacing.xs"
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "LIVE",
                                                    "variant": "labelSmall",
                                                    "weight": "bold",
                                                    "color": "token:nba.label-inverted.primary"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "id": "slot-3",
                                    "widthMode": "fill",
                                    "gap": "token:nba.spacing.md",
                                    "cornerRadius": "token:nba.radius.md",
                                    "background": "token:nba.bg.primary",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.md",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "22:00",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.secondary"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "crossAlignment": "start",
                                            "widthMode": "fill",
                                            "flex": 1.0,
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "NBA Inside Stuff",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Post-game interviews and highlights",
                                                    "variant": "bodySmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:nbatv-schedule",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": "token:nba.bg.tertiary",
                "cornerRadius": "token:nba.radius.md"
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-subscribebanner--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SubscribeBanner (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "SubscribeBanner (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:subscribe-banner~type=SubscribeBanner",
            "type": "SubscribeBanner",
            "contentSourceId": "demo:subscribe-banner",
            "analyticsId": "demo_subscribe_banner",
            "refreshPolicy": {
                "type": "static"
            },
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": {
                    "colors": [
                        "token:nba.label.accent.brand",
                        "#862633"
                    ],
                    "direction": "vertical"
                },
                "cornerRadius": "token:nba.radius.md",
                "shadow": {
                    "color": "#00000014",
                    "radius": 6,
                    "offsetX": 0,
                    "offsetY": 2
                },
                "padding": {
                    "top": 20,
                    "end": 20,
                    "bottom": 20,
                    "start": 20
                }
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "alignment": "start",
                    "crossAlignment": "start",
                    "gap": "token:nba.spacing.sm",
                    "children": [
                        {
                            "type": "Text",
                            "content": "Never Miss a Game",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label-dark.primary",
                            "accessibility": {
                                "label": "Never Miss a Game",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        },
                        {
                            "type": "Text",
                            "content": "Stream every out-of-market game live with NBA League Pass.",
                            "variant": "bodySmall",
                            "color": "token:nba.label-dark.primary"
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.md"
                        },
                        {
                            "type": "Button",
                            "label": "Subscribe Now",
                            "variant": "primary",
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://subscribe",
                                    "presentation": "modal"
                                }
                            ]
                        }
                    ]
                },
                "ctaAction": {
                    "trigger": "onActivate",
                    "type": "navigate",
                    "targetUri": "nba://subscribe",
                    "presentation": "modal"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-subscribehero--semantic-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SubscribeHero (Semantic)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "SubscribeHero (Semantic)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:subscribe-hero~type=SubscribeHero",
            "type": "SubscribeHero",
            "contentSourceId": "demo:subscribe-hero",
            "analyticsId": "demo_subscribe_hero",
            "refreshPolicy": {
                "type": "static"
            },
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": {
                    "colors": [
                        "token:nba.bg.splash-screen",
                        "token:nba.bg.splash-screen"
                    ],
                    "direction": "vertical"
                },
                "cornerRadius": "token:nba.radius.md",
                "shadow": {
                    "color": "#00000014",
                    "radius": 6,
                    "offsetX": 0,
                    "offsetY": 2
                },
                "padding": {
                    "top": 24,
                    "end": 24,
                    "bottom": 24,
                    "start": 24
                }
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "alignment": "start",
                    "crossAlignment": "center",
                    "gap": 8,
                    "widthMode": "fill",
                    "children": [
                        {
                            "type": "Image",
                            "src": "/sdui-demo/logo-wide.png",
                            "height": 64,
                            "fit": "contain",
                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                            "accessibility": {
                                "hidden": true
                            }
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.md"
                        },
                        {
                            "type": "Text",
                            "content": "NBA League Pass",
                            "variant": "headlineMedium",
                            "weight": "bold",
                            "color": "token:nba.label-dark.primary",
                            "accessibility": {
                                "label": "NBA League Pass",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        },
                        {
                            "type": "Text",
                            "content": "Watch every game. Your way.",
                            "variant": "bodyLarge",
                            "color": "token:nba.label-dark.primary"
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.lg"
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "alignment": "start",
                            "crossAlignment": "start",
                            "gap": 8,
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "start",
                                    "crossAlignment": "center",
                                    "gap": 8,
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "✓",
                                            "variant": "bodyLarge",
                                            "weight": "bold",
                                            "color": "token:nba.color.feedback.success.70"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Live & on-demand out-of-market games",
                                            "variant": "bodyLarge",
                                            "color": "token:nba.label-dark.primary",
                                            "flex": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "start",
                                    "crossAlignment": "center",
                                    "gap": 8,
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "✓",
                                            "variant": "bodyLarge",
                                            "weight": "bold",
                                            "color": "token:nba.color.feedback.success.70"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Multiple viewing angles and condensed replays",
                                            "variant": "bodyLarge",
                                            "color": "token:nba.label-dark.primary",
                                            "flex": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "start",
                                    "crossAlignment": "center",
                                    "gap": 8,
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "✓",
                                            "variant": "bodyLarge",
                                            "weight": "bold",
                                            "color": "token:nba.color.feedback.success.70"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "NBA TV included",
                                            "variant": "bodyLarge",
                                            "color": "token:nba.label-dark.primary",
                                            "flex": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "start",
                                    "crossAlignment": "center",
                                    "gap": 8,
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "✓",
                                            "variant": "bodyLarge",
                                            "weight": "bold",
                                            "color": "token:nba.color.feedback.success.70"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Compatible with all major devices",
                                            "variant": "bodyLarge",
                                            "color": "token:nba.label-dark.primary",
                                            "flex": 1
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Spacer",
                            "height": 20
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "alignment": "start",
                            "crossAlignment": "stretch",
                            "gap": "token:nba.spacing.lg",
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "start",
                                    "crossAlignment": "start",
                                    "gap": 6,
                                    "background": "token:nba.color.t-white.10",
                                    "cornerRadius": "token:nba.radius.lg",
                                    "padding": {
                                        "start": 22,
                                        "end": 22,
                                        "top": 20,
                                        "bottom": 20
                                    },
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "MOST POPULAR",
                                            "variant": "labelMedium",
                                            "weight": "bold",
                                            "color": "token:nba.color.secondary.70"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "League Pass",
                                            "variant": "titleLarge",
                                            "weight": "bold",
                                            "color": "token:nba.label-dark.primary",
                                            "accessibility": {
                                                "label": "League Pass",
                                                "role": "heading",
                                                "headingLevel": 3
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "$14.99/mo",
                                            "variant": "headlineSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "$22.99/mo",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.color.t-white.60"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• All out-of-market games",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• 3 concurrent streams",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• HD quality",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 10
                                        },
                                        {
                                            "type": "Button",
                                            "label": "Start Free Trial",
                                            "variant": "primary",
                                            "actions": [
                                                {
                                                    "trigger": "onActivate",
                                                    "type": "navigate",
                                                    "targetUri": "nba://subscribe/standard"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "start",
                                    "crossAlignment": "start",
                                    "gap": 6,
                                    "background": "token:nba.color.t-white.10",
                                    "cornerRadius": "token:nba.radius.lg",
                                    "padding": {
                                        "start": 22,
                                        "end": 22,
                                        "top": 20,
                                        "bottom": 20
                                    },
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "BEST VALUE",
                                            "variant": "labelMedium",
                                            "weight": "bold",
                                            "color": "token:nba.color.secondary.70"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "League Pass Premium",
                                            "variant": "titleLarge",
                                            "weight": "bold",
                                            "color": "token:nba.label-dark.primary",
                                            "accessibility": {
                                                "label": "League Pass Premium",
                                                "role": "heading",
                                                "headingLevel": 3
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "$22.99/mo",
                                            "variant": "headlineSmall",
                                            "weight": "bold",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• Everything in League Pass",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• No ads on VOD",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• Unlimited concurrent streams",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "• In-arena camera angles",
                                            "variant": "bodyMedium",
                                            "color": "token:nba.label-dark.primary"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 10
                                        },
                                        {
                                            "type": "Button",
                                            "label": "Go Premium",
                                            "variant": "primary",
                                            "actions": [
                                                {
                                                    "trigger": "onActivate",
                                                    "type": "navigate",
                                                    "targetUri": "nba://subscribe/premium"
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "tiers": [
                    {
                        "id": "tier-standard",
                        "name": "League Pass",
                        "price": "$14.99/mo"
                    },
                    {
                        "id": "tier-premium",
                        "name": "League Pass Premium",
                        "price": "$22.99/mo"
                    }
                ]
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-followingrail--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "FollowingRail (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "FollowingRail (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:following-rail~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_following_rail",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Following",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.sm",
                                "bottom": "token:nba.spacing.sm"
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": "token:nba.spacing.lg",
                            "showIndicators": false,
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "team-lal",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://team/1610612747"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "cornerRadius": "token:nba.radius.lg",
                                            "accessibility": {
                                                "label": "Lakers avatar",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Lakers",
                                            "variant": "labelSmall",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "team-bos",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://team/1610612738"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "cornerRadius": "token:nba.radius.lg",
                                            "accessibility": {
                                                "label": "Celtics avatar",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Celtics",
                                            "variant": "labelSmall",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "player-luka",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://player/203999"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/headshot.png?v=hs203999",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "cornerRadius": "token:nba.radius.lg",
                                            "accessibility": {
                                                "label": "Luka Dončić avatar",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Luka Dončić",
                                            "variant": "labelSmall",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "team-gsw",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://team/1610612744"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612744/primary/D/512x512/logo.png",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "cornerRadius": "token:nba.radius.lg",
                                            "accessibility": {
                                                "label": "Warriors avatar",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Warriors",
                                            "variant": "labelSmall",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "player-sga",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://player/1630175"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/headshot.png?v=hs1630175",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "/sdui-demo/placeholder-tiny.png",
                                            "cornerRadius": "token:nba.radius.lg",
                                            "accessibility": {
                                                "label": "Shai Gilgeous-Alexander avatar",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Shai Gilgeous-Alexander",
                                            "variant": "labelSmall",
                                            "maxLines": 1
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:following-rail",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-displaygrid--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "DisplayGrid (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "DisplayGrid (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:display-grid~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_display_grid",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "stretch",
                    "widthMode": "fill",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Eastern Conference Standings",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.md",
                                "bottom": "token:nba.spacing.md"
                            }
                        },
                        {
                            "type": "DisplayGrid",
                            "id": "demo:display-grid~type=AtomicComposite-grid",
                            "widthMode": "fill",
                            "striped": false,
                            "columns": [
                                {
                                    "key": "team",
                                    "label": "Team",
                                    "align": "start"
                                },
                                {
                                    "key": "w",
                                    "label": "W",
                                    "align": "center"
                                },
                                {
                                    "key": "l",
                                    "label": "L",
                                    "align": "center"
                                },
                                {
                                    "key": "pct",
                                    "label": "PCT",
                                    "align": "center"
                                },
                                {
                                    "key": "gb",
                                    "label": "GB",
                                    "align": "center"
                                },
                                {
                                    "key": "strk",
                                    "label": "STRK",
                                    "align": "center"
                                }
                            ],
                            "rows": [
                                {
                                    "team": "BOS",
                                    "w": "52",
                                    "l": "14",
                                    "pct": ".788",
                                    "gb": "-",
                                    "strk": "W5"
                                },
                                {
                                    "team": "CLE",
                                    "w": "49",
                                    "l": "17",
                                    "pct": ".742",
                                    "gb": "3.0",
                                    "strk": "W2"
                                },
                                {
                                    "team": "NYK",
                                    "w": "44",
                                    "l": "22",
                                    "pct": ".667",
                                    "gb": "8.0",
                                    "strk": "L1"
                                },
                                {
                                    "team": "MIL",
                                    "w": "42",
                                    "l": "24",
                                    "pct": ".636",
                                    "gb": "10.0",
                                    "strk": "W3"
                                },
                                {
                                    "team": "ORL",
                                    "w": "39",
                                    "l": "27",
                                    "pct": ".591",
                                    "gb": "13.0",
                                    "strk": "L2"
                                },
                                {
                                    "team": "IND",
                                    "w": "38",
                                    "l": "28",
                                    "pct": ".576",
                                    "gb": "14.0",
                                    "strk": "W1"
                                },
                                {
                                    "team": "PHI",
                                    "w": "35",
                                    "l": "31",
                                    "pct": ".530",
                                    "gb": "17.0",
                                    "strk": "L3"
                                },
                                {
                                    "team": "MIA",
                                    "w": "33",
                                    "l": "33",
                                    "pct": ".500",
                                    "gb": "19.0",
                                    "strk": "W1"
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:display-grid",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-errorstate--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "ErrorState (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "ErrorState (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:error-state~type=AtomicComposite",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "alignment": "center",
                    "crossAlignment": "center",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.xl",
                        "bottom": "token:nba.spacing.xl"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "⚠️",
                            "variant": "titleLarge"
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.md"
                        },
                        {
                            "type": "Text",
                            "content": "Something went wrong",
                            "variant": "titleMedium",
                            "weight": "bold"
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.md"
                        },
                        {
                            "type": "Text",
                            "content": "We couldn't load this content. This is a demo of the ErrorState section type.",
                            "variant": "bodyMedium",
                            "color": "token:nba.label.secondary"
                        },
                        {
                            "type": "Spacer",
                            "height": "token:nba.spacing.lg"
                        },
                        {
                            "type": "Button",
                            "label": "Try Again",
                            "variant": "primary",
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://scoreboard"
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:error-state",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-sectionslot--adslot-in-atomic-tree---composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SectionSlot (AdSlot in atomic tree) (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "SectionSlot (AdSlot in atomic tree) (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:section-slot~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo-section-slot",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "background": "token:nba.bg.primary",
                    "cornerRadius": "token:nba.radius.md",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "LAL vs BOS",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label.primary",
                            "accessibility": {
                                "label": "LAL vs BOS",
                                "role": "heading",
                                "headingLevel": 3
                            }
                        },
                        {
                            "type": "Text",
                            "content": "Q3 5:42 • LAL 87 - BOS 82",
                            "variant": "bodySmall",
                            "color": "token:nba.label.tertiary"
                        },
                        {
                            "type": "Divider",
                            "color": "token:nba.divider.moderate",
                            "thickness": 1
                        },
                        {
                            "type": "SectionSlot",
                            "id": "inline-ad",
                            "section": {
                                "id": "ads:gam-demo-inline~type=AdSlot",
                                "type": "AdSlot",
                                "contentSourceId": "ads:gam-demo-inline",
                                "surface": {
                                    "margin": {
                                        "top": "token:nba.spacing.lg",
                                        "end": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.lg",
                                        "start": "token:nba.spacing.lg"
                                    },
                                    "padding": {
                                        "top": "token:nba.spacing.lg",
                                        "end": "token:nba.spacing.md",
                                        "bottom": "token:nba.spacing.lg",
                                        "start": "token:nba.spacing.md"
                                    },
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": 0
                                },
                                "data": {
                                    "provider": "gam",
                                    "adUnitPath": "/nba/game-card-inline",
                                    "sizes": [
                                        [
                                            320,
                                            50
                                        ]
                                    ],
                                    "collapseOnEmpty": true,
                                    "label": "Advertisement",
                                    "placeholder": {
                                        "backgroundColor": "token:nba.bg.tertiary",
                                        "text": "Advertisement"
                                    }
                                },
                                "refreshPolicy": {
                                    "type": "static"
                                },
                                "sectionStates": {
                                    "error": {
                                        "hideOnError": true
                                    }
                                }
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "demo:section-slot",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-sectionheadercomposite",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SectionHeaderComposite",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "SectionHeaderComposite",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=section-header",
            "type": "AtomicComposite",
            "analyticsId": "demo_section_header_composite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "column",
                            "crossAlignment": "start",
                            "children": [
                                {
                                    "type": "Text",
                                    "content": "TOP STORIES",
                                    "variant": "titleMedium",
                                    "weight": "bold",
                                    "color": "token:nba.label.primary",
                                    "accessibility": {
                                        "label": "Top Stories",
                                        "role": "heading",
                                        "headingLevel": 2
                                    }
                                },
                                {
                                    "type": "Text",
                                    "content": "Fresh from around the league",
                                    "variant": "bodySmall",
                                    "color": "token:nba.label.tertiary",
                                    "maxLines": 1
                                }
                            ]
                        },
                        {
                            "type": "Button",
                            "label": "More >",
                            "variant": "text",
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://news"
                                }
                            ],
                            "color": "token:nba.label.accent.brand"
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.md"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-storycirclerail--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "StoryCircleRail (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "StoryCircleRail (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:story-circle-rail~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_story_circle_rail",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": 14,
                            "showIndicators": false,
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": 0,
                                "bottom": 0
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "story-finals",
                                    "width": 82,
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://stories/finals"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Finals",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 76,
                                            "height": 76,
                                            "cornerRadius": 38,
                                            "background": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": 3,
                                                "end": 3,
                                                "top": 3,
                                                "bottom": 3
                                            },
                                            "children": [
                                                {
                                                    "type": "OverlayContainer",
                                                    "base": {
                                                        "type": "Image",
                                                        "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                                                        "width": 70,
                                                        "height": 70,
                                                        "fit": "cover",
                                                        "cornerRadius": 35,
                                                        "background": "token:nba.bg.tertiary"
                                                    },
                                                    "overlays": [
                                                        {
                                                            "alignment": "bottomCenter",
                                                            "inset": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": 0
                                                            },
                                                            "element": {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "LIVE",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            }
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Finals",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "story-lakers",
                                    "width": 82,
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://stories/lakers"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Lakers",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 76,
                                            "height": 76,
                                            "cornerRadius": 38,
                                            "background": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": 3,
                                                "end": 3,
                                                "top": 3,
                                                "bottom": 3
                                            },
                                            "children": [
                                                {
                                                    "type": "OverlayContainer",
                                                    "base": {
                                                        "type": "Image",
                                                        "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png",
                                                        "width": 70,
                                                        "height": 70,
                                                        "fit": "cover",
                                                        "cornerRadius": 35,
                                                        "background": "token:nba.bg.tertiary"
                                                    },
                                                    "overlays": [
                                                        {
                                                            "alignment": "bottomCenter",
                                                            "inset": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": 0
                                                            },
                                                            "element": {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "NEW",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            }
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Lakers",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "story-draft",
                                    "width": 82,
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://stories/draft"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Draft",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 76,
                                            "height": 76,
                                            "cornerRadius": 38,
                                            "background": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": 3,
                                                "end": 3,
                                                "top": 3,
                                                "bottom": 3
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/card-wide.png?v=kwdraft",
                                                    "width": 70,
                                                    "height": 70,
                                                    "fit": "cover",
                                                    "cornerRadius": 35,
                                                    "background": "token:nba.bg.tertiary"
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Draft",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "story-nbatv",
                                    "width": 82,
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://watch/nbatv"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "NBA TV",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 76,
                                            "height": 76,
                                            "cornerRadius": 38,
                                            "background": "token:nba.label.accent.live",
                                            "padding": {
                                                "start": 3,
                                                "end": 3,
                                                "top": 3,
                                                "bottom": 3
                                            },
                                            "children": [
                                                {
                                                    "type": "OverlayContainer",
                                                    "base": {
                                                        "type": "Image",
                                                        "src": "/sdui-demo/card-wide.png?v=kwnbatv",
                                                        "width": 70,
                                                        "height": 70,
                                                        "fit": "cover",
                                                        "cornerRadius": 35,
                                                        "background": "token:nba.bg.tertiary"
                                                    },
                                                    "overlays": [
                                                        {
                                                            "alignment": "bottomCenter",
                                                            "inset": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": 0
                                                            },
                                                            "element": {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "LIVE",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            }
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": "token:nba.spacing.sm"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "NBA TV",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:story-circle-rail",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-featuredlivegamehero--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "FeaturedLiveGameHero (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "FeaturedLiveGameHero (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:featured-live-game-hero~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_featured_live_game_hero",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": "token:nba.spacing.md",
                            "showIndicators": false,
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": 0,
                                "bottom": 0
                            },
                            "paging": true,
                            "snapAlignment": "center",
                            "pageIndicator": {
                                "style": "dots",
                                "alignment": "bottomCenter",
                                "color": "token:nba.label.tertiary",
                                "activeColor": "token:nba.label-inverted.primary"
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "crossAlignment": "stretch",
                                    "id": "hero-game-1",
                                    "width": 338,
                                    "cornerRadius": 0,
                                    "background": "token:nba.bg.secondary",
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://game/0022400777"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Lakers at Celtics",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "OverlayContainer",
                                            "base": {
                                                "type": "Image",
                                                "src": "/sdui-demo/card-wide.png?v=kwhero-lal-bos",
                                                "fit": "cover",
                                                "widthMode": "fill",
                                                "aspectRatio": 1.7777777777777777,
                                                "accessibility": {
                                                    "label": "Lakers at Celtics",
                                                    "role": "image"
                                                }
                                            },
                                            "overlays": [
                                                {
                                                    "alignment": "bottomStart",
                                                    "element": {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "alignment": "end",
                                                        "crossAlignment": "start",
                                                        "widthMode": "fill",
                                                        "heightMode": "fill",
                                                        "padding": {
                                                            "start": "token:nba.spacing.lg",
                                                            "end": "token:nba.spacing.lg",
                                                            "top": "token:nba.spacing.md",
                                                            "bottom": "token:nba.spacing.md"
                                                        },
                                                        "background": {
                                                            "colors": [
                                                                "#00000000",
                                                                "#99000000",
                                                                "#F0000000"
                                                            ],
                                                            "direction": "vertical"
                                                        },
                                                        "children": [
                                                            {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "LIVE",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "type": "Spacer",
                                                                "height": "token:nba.spacing.sm"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Lakers at Celtics",
                                                                "variant": "titleMedium",
                                                                "weight": "bold",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 2
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Fourth-quarter finish on NBA TV",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 2
                                                            }
                                                        ]
                                                    }
                                                },
                                                {
                                                    "alignment": "topEnd",
                                                    "inset": {
                                                        "start": "token:nba.spacing.sm",
                                                        "end": "token:nba.spacing.sm",
                                                        "top": 0,
                                                        "bottom": 0
                                                    },
                                                    "element": {
                                                        "type": "Button",
                                                        "label": "⋯",
                                                        "variant": "text",
                                                        "color": "token:nba.label-dark.primary",
                                                        "actions": [
                                                            {
                                                                "trigger": "onActivate",
                                                                "type": "navigate",
                                                                "targetUri": "nba://game/0022400777/actions"
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "spaceBetween",
                                            "crossAlignment": "center",
                                            "widthMode": "fill",
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": "token:nba.spacing.md",
                                                "bottom": "token:nba.spacing.sm"
                                            },
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "gap": "token:nba.spacing.sm",
                                                    "width": 112,
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "column",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "children": [
                                                                {
                                                                    "type": "Image",
                                                                    "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png",
                                                                    "width": 44,
                                                                    "height": 44,
                                                                    "fit": "contain",
                                                                    "accessibility": {
                                                                        "label": "LAL logo",
                                                                        "role": "image"
                                                                    }
                                                                },
                                                                {
                                                                    "type": "Text",
                                                                    "content": "LAL",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label.primary",
                                                                    "maxLines": 1
                                                                }
                                                            ]
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "89",
                                                            "variant": "titleLarge",
                                                            "weight": "bold",
                                                            "color": "token:nba.label.primary",
                                                            "maxLines": 1,
                                                            "bindRef": "cards.hero-game-1.awayScore",
                                                            "monospacedDigits": true
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "width": 88,
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "gap": "token:nba.spacing.xs",
                                                            "children": [
                                                                {
                                                                    "type": "Container",
                                                                    "direction": "row",
                                                                    "alignment": "center",
                                                                    "crossAlignment": "center",
                                                                    "width": 6,
                                                                    "height": 6,
                                                                    "cornerRadius": "token:nba.radius.sm",
                                                                    "background": "token:nba.label.accent.live",
                                                                    "children": []
                                                                },
                                                                {
                                                                    "type": "Text",
                                                                    "content": "Q4 2:15",
                                                                    "variant": "labelSmall",
                                                                    "weight": "semiBold",
                                                                    "color": "token:nba.label.secondary",
                                                                    "maxLines": 1,
                                                                    "bindRef": "cards.hero-game-1.statusText"
                                                                }
                                                            ]
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "BOS leads 3-2",
                                                            "variant": "labelSmall",
                                                            "color": "token:nba.label.tertiary",
                                                            "maxLines": 1
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "gap": "token:nba.spacing.sm",
                                                    "width": 108,
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "94",
                                                            "variant": "titleLarge",
                                                            "weight": "bold",
                                                            "color": "token:nba.label.primary",
                                                            "maxLines": 1,
                                                            "bindRef": "cards.hero-game-1.homeScore",
                                                            "monospacedDigits": true
                                                        },
                                                        {
                                                            "type": "Container",
                                                            "direction": "column",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "children": [
                                                                {
                                                                    "type": "Image",
                                                                    "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                                                                    "width": 44,
                                                                    "height": 44,
                                                                    "fit": "contain",
                                                                    "accessibility": {
                                                                        "label": "BOS logo",
                                                                        "role": "image"
                                                                    }
                                                                },
                                                                {
                                                                    "type": "Text",
                                                                    "content": "BOS",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label.primary",
                                                                    "maxLines": 1
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Divider",
                                            "thickness": 1,
                                            "color": "token:nba.divider.subtle"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "end",
                                            "crossAlignment": "center",
                                            "gap": "token:nba.spacing.sm",
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": 0,
                                                "bottom": "token:nba.spacing.md"
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/logo-wide.png",
                                                    "width": 48,
                                                    "height": 20,
                                                    "fit": "contain"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "crossAlignment": "stretch",
                                    "id": "hero-game-2",
                                    "width": 338,
                                    "cornerRadius": 0,
                                    "background": "token:nba.bg.secondary",
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://game/0022400778"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Thunder at Nuggets",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "OverlayContainer",
                                            "base": {
                                                "type": "Image",
                                                "src": "/sdui-demo/card-wide.png?v=kwhero-okc-den",
                                                "fit": "cover",
                                                "widthMode": "fill",
                                                "aspectRatio": 1.7777777777777777,
                                                "accessibility": {
                                                    "label": "Thunder at Nuggets",
                                                    "role": "image"
                                                }
                                            },
                                            "overlays": [
                                                {
                                                    "alignment": "bottomStart",
                                                    "element": {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "alignment": "end",
                                                        "crossAlignment": "start",
                                                        "widthMode": "fill",
                                                        "heightMode": "fill",
                                                        "padding": {
                                                            "start": "token:nba.spacing.lg",
                                                            "end": "token:nba.spacing.lg",
                                                            "top": "token:nba.spacing.md",
                                                            "bottom": "token:nba.spacing.md"
                                                        },
                                                        "background": {
                                                            "colors": [
                                                                "#00000000",
                                                                "#99000000",
                                                                "#F0000000"
                                                            ],
                                                            "direction": "vertical"
                                                        },
                                                        "children": [
                                                            {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "UP NEXT",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "type": "Spacer",
                                                                "height": "token:nba.spacing.sm"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Thunder at Nuggets",
                                                                "variant": "titleMedium",
                                                                "weight": "bold",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 2
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Coverage begins at 10:00 PM ET",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 2
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "spaceBetween",
                                            "crossAlignment": "center",
                                            "widthMode": "fill",
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": "token:nba.spacing.md",
                                                "bottom": "token:nba.spacing.sm"
                                            },
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "gap": "token:nba.spacing.sm",
                                                    "width": 112,
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "column",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "children": [
                                                                {
                                                                    "type": "Image",
                                                                    "src": "https://cdn.nba.com/logos/nba/1610612760/primary/D/512x512/logo.png",
                                                                    "width": 44,
                                                                    "height": 44,
                                                                    "fit": "contain",
                                                                    "accessibility": {
                                                                        "label": "OKC logo",
                                                                        "role": "image"
                                                                    }
                                                                },
                                                                {
                                                                    "type": "Text",
                                                                    "content": "OKC",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label.primary",
                                                                    "maxLines": 1
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "width": 88,
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "row",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "gap": "token:nba.spacing.xs",
                                                            "children": [
                                                                {
                                                                    "type": "Container",
                                                                    "direction": "row",
                                                                    "alignment": "center",
                                                                    "crossAlignment": "center",
                                                                    "width": 6,
                                                                    "height": 6,
                                                                    "cornerRadius": "token:nba.radius.sm",
                                                                    "background": "token:nba.label.accent.live",
                                                                    "children": []
                                                                },
                                                                {
                                                                    "type": "Text",
                                                                    "content": "10:00 PM ET",
                                                                    "variant": "labelSmall",
                                                                    "weight": "semiBold",
                                                                    "color": "token:nba.label.secondary",
                                                                    "maxLines": 1,
                                                                    "bindRef": "cards.hero-game-2.statusText"
                                                                }
                                                            ]
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "Season series tied",
                                                            "variant": "labelSmall",
                                                            "color": "token:nba.label.tertiary",
                                                            "maxLines": 1
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "gap": "token:nba.spacing.sm",
                                                    "width": 108,
                                                    "children": [
                                                        {
                                                            "type": "Container",
                                                            "direction": "column",
                                                            "alignment": "center",
                                                            "crossAlignment": "center",
                                                            "children": [
                                                                {
                                                                    "type": "Image",
                                                                    "src": "https://cdn.nba.com/logos/nba/1610612743/primary/D/512x512/logo.png",
                                                                    "width": 44,
                                                                    "height": 44,
                                                                    "fit": "contain",
                                                                    "accessibility": {
                                                                        "label": "DEN logo",
                                                                        "role": "image"
                                                                    }
                                                                },
                                                                {
                                                                    "type": "Text",
                                                                    "content": "DEN",
                                                                    "variant": "labelSmall",
                                                                    "weight": "bold",
                                                                    "color": "token:nba.label.primary",
                                                                    "maxLines": 1
                                                                }
                                                            ]
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Divider",
                                            "thickness": 1,
                                            "color": "token:nba.divider.subtle"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "end",
                                            "crossAlignment": "center",
                                            "gap": "token:nba.spacing.sm",
                                            "padding": {
                                                "start": "token:nba.spacing.lg",
                                                "end": "token:nba.spacing.lg",
                                                "top": 0,
                                                "bottom": "token:nba.spacing.md"
                                            },
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "/sdui-demo/logo-wide.png",
                                                    "width": 48,
                                                    "height": 20,
                                                    "fit": "contain"
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "content": {
                    "cards": {
                        "hero-game-1": {
                            "awayScore": 89,
                            "homeScore": 94,
                            "statusText": "Q4 2:15"
                        },
                        "hero-game-2": {
                            "awayScore": 0,
                            "homeScore": 0,
                            "statusText": "10:00 PM ET"
                        }
                    }
                }
            },
            "contentSourceId": "demo:featured-live-game-hero",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-editorialoverlayrail--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "EditorialOverlayRail (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "EditorialOverlayRail (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:editorial-overlay-rail~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_editorial_overlay_rail",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": 14,
                            "showIndicators": false,
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": 0,
                                "bottom": 0
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "editorial-1",
                                    "width": 200,
                                    "cornerRadius": "token:nba.radius.md",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://article/five-things"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Five things to watch tonight",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "OverlayContainer",
                                            "base": {
                                                "type": "Image",
                                                "src": "/sdui-demo/card-wide.png?v=kweditorial-1",
                                                "width": 200,
                                                "fit": "cover",
                                                "widthMode": "fill",
                                                "aspectRatio": 0.75,
                                                "cornerRadius": "token:nba.radius.md",
                                                "accessibility": {
                                                    "label": "Five things to watch tonight",
                                                    "role": "image"
                                                }
                                            },
                                            "overlays": [
                                                {
                                                    "alignment": "bottomStart",
                                                    "element": {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "alignment": "end",
                                                        "crossAlignment": "start",
                                                        "widthMode": "fill",
                                                        "heightMode": "fill",
                                                        "padding": {
                                                            "start": "token:nba.spacing.md",
                                                            "end": "token:nba.spacing.md",
                                                            "top": "token:nba.spacing.md",
                                                            "bottom": "token:nba.spacing.md"
                                                        },
                                                        "background": {
                                                            "colors": [
                                                                "#00000000",
                                                                "#99000000",
                                                                "#F0000000"
                                                            ],
                                                            "direction": "vertical"
                                                        },
                                                        "cornerRadii": {
                                                            "topStart": 0,
                                                            "topEnd": 0,
                                                            "bottomStart": "token:nba.radius.md",
                                                            "bottomEnd": "token:nba.radius.md"
                                                        },
                                                        "children": [
                                                            {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "NEW",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "type": "Spacer",
                                                                "height": "token:nba.spacing.sm"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Five things to watch tonight",
                                                                "variant": "titleSmall",
                                                                "weight": "bold",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 3
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Rivalries, returns, and playoff stakes",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 3
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "editorial-2",
                                    "width": 200,
                                    "cornerRadius": "token:nba.radius.md",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://article/mvp-race"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Inside the MVP race",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "OverlayContainer",
                                            "base": {
                                                "type": "Image",
                                                "src": "/sdui-demo/card-wide.png?v=kweditorial-2",
                                                "width": 200,
                                                "fit": "cover",
                                                "widthMode": "fill",
                                                "aspectRatio": 0.75,
                                                "cornerRadius": "token:nba.radius.md",
                                                "accessibility": {
                                                    "label": "Inside the MVP race",
                                                    "role": "image"
                                                }
                                            },
                                            "overlays": [
                                                {
                                                    "alignment": "bottomStart",
                                                    "element": {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "alignment": "end",
                                                        "crossAlignment": "start",
                                                        "widthMode": "fill",
                                                        "heightMode": "fill",
                                                        "padding": {
                                                            "start": "token:nba.spacing.md",
                                                            "end": "token:nba.spacing.md",
                                                            "top": "token:nba.spacing.md",
                                                            "bottom": "token:nba.spacing.md"
                                                        },
                                                        "background": {
                                                            "colors": [
                                                                "#00000000",
                                                                "#99000000",
                                                                "#F0000000"
                                                            ],
                                                            "direction": "vertical"
                                                        },
                                                        "cornerRadii": {
                                                            "topStart": 0,
                                                            "topEnd": 0,
                                                            "bottomStart": "token:nba.radius.md",
                                                            "bottomEnd": "token:nba.radius.md"
                                                        },
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "Inside the MVP race",
                                                                "variant": "titleSmall",
                                                                "weight": "bold",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 3
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "The numbers behind a tight finish",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 3
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "editorial-3",
                                    "width": 200,
                                    "cornerRadius": "token:nba.radius.md",
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://video/rookies"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Rookies making noise",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "OverlayContainer",
                                            "base": {
                                                "type": "Image",
                                                "src": "/sdui-demo/card-wide.png?v=kweditorial-3",
                                                "width": 200,
                                                "fit": "cover",
                                                "widthMode": "fill",
                                                "aspectRatio": 0.75,
                                                "cornerRadius": "token:nba.radius.md",
                                                "accessibility": {
                                                    "label": "Rookies making noise",
                                                    "role": "image"
                                                }
                                            },
                                            "overlays": [
                                                {
                                                    "alignment": "bottomStart",
                                                    "element": {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "alignment": "end",
                                                        "crossAlignment": "start",
                                                        "widthMode": "fill",
                                                        "heightMode": "fill",
                                                        "padding": {
                                                            "start": "token:nba.spacing.md",
                                                            "end": "token:nba.spacing.md",
                                                            "top": "token:nba.spacing.md",
                                                            "bottom": "token:nba.spacing.md"
                                                        },
                                                        "background": {
                                                            "colors": [
                                                                "#00000000",
                                                                "#99000000",
                                                                "#F0000000"
                                                            ],
                                                            "direction": "vertical"
                                                        },
                                                        "cornerRadii": {
                                                            "topStart": 0,
                                                            "topEnd": 0,
                                                            "bottomStart": "token:nba.radius.md",
                                                            "bottomEnd": "token:nba.radius.md"
                                                        },
                                                        "children": [
                                                            {
                                                                "type": "Container",
                                                                "direction": "row",
                                                                "crossAlignment": "center",
                                                                "background": "token:nba.label.accent.live",
                                                                "cornerRadius": "token:nba.radius.sm",
                                                                "padding": {
                                                                    "start": "token:nba.spacing.xs",
                                                                    "end": "token:nba.spacing.xs",
                                                                    "top": "token:nba.spacing.xs",
                                                                    "bottom": "token:nba.spacing.xs"
                                                                },
                                                                "children": [
                                                                    {
                                                                        "type": "Text",
                                                                        "content": "LIVE",
                                                                        "variant": "labelSmall",
                                                                        "weight": "bold",
                                                                        "color": "token:nba.label-inverted.primary"
                                                                    }
                                                                ]
                                                            },
                                                            {
                                                                "type": "Spacer",
                                                                "height": "token:nba.spacing.sm"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "Rookies making noise",
                                                                "variant": "titleSmall",
                                                                "weight": "bold",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 3
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "First-year players changing rotations",
                                                                "variant": "bodySmall",
                                                                "color": "token:nba.label-dark.primary",
                                                                "maxLines": 3
                                                            }
                                                        ]
                                                    }
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:editorial-overlay-rail",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-utilitycardgrid--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "UtilityCardGrid (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "UtilityCardGrid (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:utility-card-grid~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_utility_card_grid",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "stretch",
                    "gap": "token:nba.spacing.md",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": 0,
                        "bottom": "token:nba.spacing.lg"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Around the League",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label.primary"
                        },
                        {
                            "type": "Container",
                            "direction": "row",
                            "crossAlignment": "stretch",
                            "gap": "token:nba.spacing.md",
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "utility-standings",
                                    "gap": "token:nba.spacing.sm",
                                    "height": 132,
                                    "widthMode": "fill",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://standings"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Standings",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/card-wide.png?v=kwstandings",
                                            "width": 44,
                                            "height": 44,
                                            "fit": "contain",
                                            "accessibility": {
                                                "label": "Standings icon",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Standings",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Conference and division tables",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary",
                                            "maxLines": 2
                                        }
                                    ],
                                    "flex": 1.0
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "utility-stats",
                                    "gap": "token:nba.spacing.sm",
                                    "height": 132,
                                    "widthMode": "fill",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://stats"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Stats",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 44,
                                            "height": 44,
                                            "cornerRadius": 22,
                                            "background": "token:nba.bg.tertiary",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "STA",
                                                    "variant": "labelSmall",
                                                    "weight": "bold",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Stats",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        },
                                        {
                                            "type": "Text",
                                            "content": "League leaders and team ranks",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary",
                                            "maxLines": 2
                                        }
                                    ],
                                    "flex": 1.0
                                }
                            ]
                        },
                        {
                            "type": "Container",
                            "direction": "row",
                            "crossAlignment": "stretch",
                            "gap": "token:nba.spacing.md",
                            "widthMode": "fill",
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "utility-tickets",
                                    "gap": "token:nba.spacing.sm",
                                    "height": 132,
                                    "widthMode": "fill",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://tickets"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Tickets",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 44,
                                            "height": 44,
                                            "cornerRadius": 22,
                                            "background": "token:nba.bg.tertiary",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "TIC",
                                                    "variant": "labelSmall",
                                                    "weight": "bold",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Tickets",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Find games near you",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary",
                                            "maxLines": 2
                                        }
                                    ],
                                    "flex": 1.0
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "utility-shop",
                                    "gap": "token:nba.spacing.sm",
                                    "height": 132,
                                    "widthMode": "fill",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.md"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://shop"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "Shop",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/card-wide.png?v=kwshop",
                                            "width": 44,
                                            "height": 44,
                                            "fit": "contain",
                                            "accessibility": {
                                                "label": "Shop icon",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Shop",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Jerseys, hats, and more",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.secondary",
                                            "maxLines": 2
                                        }
                                    ],
                                    "flex": 1.0
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:utility-card-grid",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "end": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg",
                    "start": "token:nba.spacing.lg"
                },
                "background": "token:nba.bg.tertiary",
                "cornerRadius": "token:nba.radius.md"
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-leaguecardrail--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "LeagueCardRail (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "LeagueCardRail (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:league-card-rail~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_league_card_rail",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 0,
                        "end": 0,
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Other Leagues",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label.primary",
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.xs",
                                "bottom": "token:nba.spacing.sm"
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": "token:nba.spacing.md",
                            "showIndicators": false,
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": 0,
                                "bottom": 0
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "league-wnba",
                                    "width": 160,
                                    "gap": "token:nba.spacing.sm",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.lg"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://league/wnba"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "WNBA",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/card-wide.png?v=kwleague-wnba",
                                            "width": 72,
                                            "height": 56,
                                            "fit": "contain",
                                            "aspectRatio": 1.3333333333333333,
                                            "accessibility": {
                                                "label": "WNBA logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "WNBA",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "league-gleague",
                                    "width": 160,
                                    "gap": "token:nba.spacing.sm",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.lg"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://league/gleague"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "G League",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "/sdui-demo/card-wide.png?v=kwleague-gleague",
                                            "width": 72,
                                            "height": 56,
                                            "fit": "contain",
                                            "aspectRatio": 1.3333333333333333,
                                            "accessibility": {
                                                "label": "G League logo",
                                                "role": "image"
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "G League",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "league-2k",
                                    "width": 160,
                                    "gap": "token:nba.spacing.sm",
                                    "background": "token:nba.bg.secondary",
                                    "cornerRadius": "token:nba.radius.md",
                                    "padding": {
                                        "start": "token:nba.spacing.md",
                                        "end": "token:nba.spacing.md",
                                        "top": "token:nba.spacing.lg",
                                        "bottom": "token:nba.spacing.lg"
                                    },
                                    "shadow": {
                                        "color": "#00000014",
                                        "radius": 4.0,
                                        "offsetX": 0.0,
                                        "offsetY": 2.0
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onActivate",
                                            "type": "navigate",
                                            "targetUri": "nba://league/2k"
                                        }
                                    ],
                                    "accessibility": {
                                        "label": "NBA 2K League",
                                        "role": "button"
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 72,
                                            "height": 72,
                                            "cornerRadius": 28,
                                            "background": "token:nba.bg.tertiary",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "NL",
                                                    "variant": "labelSmall",
                                                    "weight": "bold",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "NBA 2K League",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 2
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            },
            "contentSourceId": "demo:league-card-rail",
            "surface": {
                "margin": {
                    "top": "token:nba.spacing.lg",
                    "bottom": "token:nba.spacing.lg"
                }
            },
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "feed:demo~type=AtomicComposite~slug=label-gameschedulelist--composite-",
            "type": "AtomicComposite",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "row",
                    "alignment": "spaceBetween",
                    "crossAlignment": "center",
                    "widthMode": "fill",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": "token:nba.spacing.md",
                        "bottom": "token:nba.spacing.xs"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "GameScheduleList (Composite)",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "accessibility": {
                                "label": "GameScheduleList (Composite)",
                                "role": "heading",
                                "headingLevel": 2
                            }
                        }
                    ]
                }
            },
            "contentSourceId": "feed:demo",
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        },
        {
            "id": "demo:game-schedule-list~type=AtomicComposite",
            "type": "AtomicComposite",
            "analyticsId": "demo_game_schedule_list",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "crossAlignment": "stretch",
                    "gap": "token:nba.spacing.md",
                    "padding": {
                        "start": "token:nba.spacing.lg",
                        "end": "token:nba.spacing.lg",
                        "top": 0,
                        "bottom": "token:nba.spacing.md"
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Today",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "token:nba.label.primary"
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "crossAlignment": "stretch",
                            "id": "schedule-demo-1",
                            "gap": "token:nba.spacing.sm",
                            "background": "token:nba.bg.secondary",
                            "cornerRadius": 0,
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.lg",
                                "bottom": "token:nba.spacing.lg"
                            },
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://game/0022400001"
                                }
                            ],
                            "accessibility": {
                                "label": "NYK vs BOS, Final",
                                "role": "button"
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "spaceBetween",
                                    "crossAlignment": "center",
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 72,
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://cdn.nba.com/logos/nba/1610612752/primary/D/512x512/logo.png",
                                                    "width": 48,
                                                    "height": 48,
                                                    "fit": "contain",
                                                    "accessibility": {
                                                        "label": "NYK logo",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Spacer",
                                                    "height": "token:nba.spacing.xs"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "3 NYK",
                                                    "variant": "titleMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Knicks",
                                                    "variant": "labelSmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "109",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "schedule-demo-1.awayScore",
                                            "monospacedDigits": true,
                                            "textAlign": "center"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 88,
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Final",
                                                    "variant": "labelSmall",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 2,
                                                    "bindRef": "schedule-demo-1.statusText",
                                                    "textAlign": "center"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "BOS leads 1-0",
                                                    "variant": "labelSmall",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.tertiary",
                                                    "maxLines": 2,
                                                    "textAlign": "center"
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "132",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "schedule-demo-1.homeScore",
                                            "monospacedDigits": true,
                                            "textAlign": "center"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 72,
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://cdn.nba.com/logos/nba/1610612738/primary/D/512x512/logo.png",
                                                    "width": 48,
                                                    "height": 48,
                                                    "fit": "contain",
                                                    "accessibility": {
                                                        "label": "BOS logo",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Spacer",
                                                    "height": "token:nba.spacing.xs"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "2 BOS",
                                                    "variant": "titleMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Celtics",
                                                    "variant": "labelSmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "/sdui-demo/logo-wide.png",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.tertiary",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Divider",
                                    "thickness": 1,
                                    "color": "token:nba.divider.subtle"
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "spaceBetween",
                                    "crossAlignment": "center",
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "start",
                                            "crossAlignment": "center",
                                            "gap": "token:nba.spacing.sm",
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "width": 24,
                                                    "height": 24,
                                                    "cornerRadius": 12,
                                                    "background": "token:nba.label.accent.brand",
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "▶",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "token:nba.label-inverted.primary",
                                                            "maxLines": 1
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "League Pass",
                                                    "variant": "labelMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                }
                                            ],
                                            "actions": [
                                                {
                                                    "trigger": "onActivate",
                                                    "type": "navigate",
                                                    "targetUri": "nba://commerce/leaguepass"
                                                }
                                            ],
                                            "accessibility": {
                                                "label": "League Pass",
                                                "role": "button"
                                            }
                                        },
                                        {
                                            "type": "Button",
                                            "label": "",
                                            "icon": "sdui:more",
                                            "variant": "text",
                                            "color": "token:nba.label.secondary",
                                            "actions": [
                                                {
                                                    "trigger": "onActivate",
                                                    "type": "navigate",
                                                    "targetUri": "nba://game/0022400001/actions"
                                                }
                                            ]
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "crossAlignment": "stretch",
                            "id": "schedule-demo-2",
                            "gap": "token:nba.spacing.sm",
                            "background": "token:nba.bg.secondary",
                            "cornerRadius": 0,
                            "padding": {
                                "start": "token:nba.spacing.lg",
                                "end": "token:nba.spacing.lg",
                                "top": "token:nba.spacing.lg",
                                "bottom": "token:nba.spacing.lg"
                            },
                            "actions": [
                                {
                                    "trigger": "onActivate",
                                    "type": "navigate",
                                    "targetUri": "nba://game/0022400002"
                                }
                            ],
                            "accessibility": {
                                "label": "MIN vs LAL, Final",
                                "role": "button"
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "spaceBetween",
                                    "crossAlignment": "center",
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 72,
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://cdn.nba.com/logos/nba/1610612750/primary/D/512x512/logo.png",
                                                    "width": 48,
                                                    "height": 48,
                                                    "fit": "contain",
                                                    "accessibility": {
                                                        "label": "MIN logo",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Spacer",
                                                    "height": "token:nba.spacing.xs"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "6 MIN",
                                                    "variant": "titleMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Timberwolves",
                                                    "variant": "labelSmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "103",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "schedule-demo-2.awayScore",
                                            "monospacedDigits": true,
                                            "textAlign": "center"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 88,
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Final",
                                                    "variant": "labelSmall",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 2,
                                                    "bindRef": "schedule-demo-2.statusText",
                                                    "textAlign": "center"
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Text",
                                            "content": "110",
                                            "variant": "score",
                                            "weight": "bold",
                                            "color": "token:nba.label.primary",
                                            "maxLines": 1,
                                            "bindRef": "schedule-demo-2.homeScore",
                                            "monospacedDigits": true,
                                            "textAlign": "center"
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "alignment": "center",
                                            "crossAlignment": "center",
                                            "width": 72,
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://cdn.nba.com/logos/nba/1610612747/primary/D/512x512/logo.png",
                                                    "width": 48,
                                                    "height": 48,
                                                    "fit": "contain",
                                                    "accessibility": {
                                                        "label": "LAL logo",
                                                        "role": "image"
                                                    }
                                                },
                                                {
                                                    "type": "Spacer",
                                                    "height": "token:nba.spacing.xs"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "7 LAL",
                                                    "variant": "titleMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Lakers",
                                                    "variant": "labelSmall",
                                                    "color": "token:nba.label.secondary",
                                                    "maxLines": 1
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "/sdui-demo/logo-wide.png",
                                            "variant": "labelSmall",
                                            "color": "token:nba.label.tertiary",
                                            "maxLines": 1
                                        }
                                    ]
                                },
                                {
                                    "type": "Divider",
                                    "thickness": 1,
                                    "color": "token:nba.divider.subtle"
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "spaceBetween",
                                    "crossAlignment": "center",
                                    "widthMode": "fill",
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "row",
                                            "alignment": "start",
                                            "crossAlignment": "center",
                                            "gap": "token:nba.spacing.sm",
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "alignment": "center",
                                                    "crossAlignment": "center",
                                                    "width": 24,
                                                    "height": 24,
                                                    "cornerRadius": 12,
                                                    "background": "token:nba.label.accent.brand",
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "▶",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "token:nba.label-inverted.primary",
                                                            "maxLines": 1
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "League Pass",
                                                    "variant": "labelMedium",
                                                    "weight": "semiBold",
                                                    "color": "token:nba.label.primary",
                                                    "maxLines": 1
                                                }
                                            ],
                                            "actions": [
                                                {
                                                    "trigger": "onActivate",
                                                    "type": "navigate",
                                                    "targetUri": "nba://commerce/leaguepass"
                                                }
                                            ],
                                            "accessibility": {
                                                "label": "League Pass",
                                                "role": "button"
                                            }
                                        },
                                        {
                                            "type": "Button",
                                            "label": "",
                                            "icon": "sdui:more",
                                            "variant": "text",
                                            "color": "token:nba.label.secondary"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                },
                "content": {
                    "schedule-demo-1": {
                        "awayScore": 109,
                        "homeScore": 132,
                        "statusText": "Final"
                    },
                    "schedule-demo-2": {
                        "awayScore": 103,
                        "homeScore": 110,
                        "statusText": "Final"
                    }
                }
            },
            "contentSourceId": "demo:game-schedule-list",
            "surface": {},
            "stringTable": {
                "period.ot": "OT",
                "period.q4": "Q4",
                "period.q3": "Q3",
                "status.final": "Final",
                "period.q2": "Q2",
                "period.q1": "Q1",
                "status.pre": "Upcoming",
                "month.november": "November",
                "month.april": "April",
                "month.december": "December",
                "status.halftime": "Halftime",
                "month.march": "March",
                "month.january": "January",
                "screen.schedule": "Schedule",
                "filter.month": "Month",
                "status.live": "Live",
                "filter.apply": "Apply",
                "filter.day": "Day",
                "filter.year": "Year",
                "month.february": "February",
                "filter.season": "Season",
                "month.october": "October",
                "filter.all": "All"
            }
        }
    ],
    "contentInsets": {
        "start": "token:nba.spacing.md",
        "end": "token:nba.spacing.md",
        "bottom": "token:nba.spacing.lg"
    }
}
```
