# Kitchen Sink — Complete SDUI Response Example

> **Source**: `GET /sdui/demos` with `X-Platform: android`
>
> **Generated**: 2026-03-13 from the running composition service (DemoScreenComposer)

This is the full server response for the kitchen-sink demo screen — a single SDUI payload that exercises every section type and atomic primitive the prototype supports. It is the canonical reference for what a complete SDUI response looks like in production shape.

### What this response demonstrates

- **Dual-layer rendering** — 32 `AtomicComposite` sections (server-composed atomic trees rendered by `AtomicRouter`) alongside 10 semantic sections rendered by dedicated client components
- **All 9 atomic primitives** — Container, Text, Image, Button, Spacer, Divider, ScrollContainer, Conditional, DisplayGrid — composed into AtomicComposite trees
- **SectionSlot bridge** — atomic trees embedding full section renderers (AdSlot inside an atomic layout)
- **All semantic section types still using dedicated renderers** — GamePanel (2), BoxscoreTable, Form, TabGroup, SeasonLeadersTable, SubscribeBanner, SubscribeHero, AdSlot
- **Mixed refresh policies** — static, polling (with `intervalSec` + direct CDN `url`), and SSE (Ably channel) coexisting on the same screen
- **Action system** — navigate, fireAndForget, mutate, dismiss, refresh actions at screen, section, and subsection scopes
- **Data bindings** — field-level JSONPath bindings with `stringKeys` for i18n on SSE-updated fields
- **Screen state** — `state` object with default values, mutated by actions (tab selection, sort column/direction)
- **Navigation** — bottom navigation bar with 5 items, server-driven

### Section inventory (42 sections)

| Section type | Count | Rendering layer |
|---|---|---|
| AtomicComposite | 32 | Atomic (AtomicRouter) |
| GamePanel | 2 | Semantic (dedicated renderer) |
| BoxscoreTable | 1 | Semantic |
| Form | 1 | Semantic |
| TabGroup | 1 | Semantic |
| Row | 1 | Semantic |
| SeasonLeadersTable | 1 | Semantic |
| SubscribeBanner | 1 | Semantic |
| SubscribeHero | 1 | Semantic |
| AdSlot | 1 | Semantic |

---

## Kitchen Sink Json Example

```json
{
    "id": "demos",
    "schemaVersion": "1.0",
    "title": "SDUI Section Types",
    "analyticsId": "demos-kitchen-sink",
    "traceId": "trace-3b5e8c9e",
    "parentUri": "nba://scoreboard",
    "defaultRefreshPolicy": {
        "type": "static"
    },
    "navigation": {
        "items": [
            {
                "id": "for-you",
                "label": "For You",
                "icon": "home",
                "targetUri": "nba://for-you",
                "selected": false
            },
            {
                "id": "games",
                "label": "Games",
                "icon": "sports_basketball",
                "targetUri": "nba://games",
                "selected": false
            },
            {
                "id": "watch",
                "label": "Watch",
                "icon": "play_circle",
                "targetUri": "nba://watch",
                "selected": false
            },
            {
                "id": "leaders",
                "label": "Leaders",
                "icon": "leaderboard",
                "targetUri": "nba://leaders",
                "selected": false
            },
            {
                "id": "demos",
                "label": "Kitchen",
                "icon": "widgets",
                "targetUri": "nba://demos",
                "selected": true
            }
        ]
    },
    "sections": [
        {
            "id": "label-gamepanel (scoreboard)",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "GamePanel (scoreboard)",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-game-panel-scoreboard",
            "type": "AtomicComposite",
            "analyticsId": "demo_game_panel_scoreboard",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 8,
                        "end": 8,
                        "top": 0,
                        "bottom": 0
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "row",
                            "alignment": "spaceEvenly",
                            "crossAlignment": "center",
                            "cornerRadius": 12,
                            "backgroundColor": "#17408B",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 24,
                                "bottom": 24
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg",
                                            "width": 60,
                                            "height": 60,
                                            "fit": "contain",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
                                        },
                                        {
                                            "type": "Text",
                                            "content": "LAL",
                                            "variant": "bodyMedium",
                                            "weight": "bold",
                                            "color": "#FFFFFF"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "89",
                                            "variant": "headlineLarge",
                                            "weight": "bold",
                                            "color": "#FFFFFF"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "Q3 4:32",
                                            "variant": "bodyMedium",
                                            "weight": "medium",
                                            "color": "#FFFFFF"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Period 3",
                                            "variant": "bodySmall",
                                            "color": "#CCCCCC"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg",
                                            "width": 60,
                                            "height": 60,
                                            "fit": "contain",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
                                        },
                                        {
                                            "type": "Text",
                                            "content": "BOS",
                                            "variant": "bodyMedium",
                                            "weight": "bold",
                                            "color": "#FFFFFF"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "94",
                                            "variant": "headlineLarge",
                                            "weight": "bold",
                                            "color": "#FFFFFF"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        },
        {
            "id": "label-adslot",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "AdSlot",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-ad-slot",
            "type": "AdSlot",
            "analyticsId": "demo_ad_slot",
            "refreshPolicy": {
                "type": "static"
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
                "label": "Advertisement"
            }
        },
        {
            "id": "label-statline",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "StatLine",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-stat-line",
            "type": "AtomicComposite",
            "analyticsId": "demo_stat_line",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 8,
                        "bottom": 8
                    },
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
                                "bottom": 12
                            }
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "padding": {
                                "start": 0,
                                "end": 0,
                                "top": 6,
                                "bottom": 6
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/1628369.png",
                                            "width": 40,
                                            "height": 40,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 20
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 12
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Jayson Tatum",
                                                    "variant": "bodyLarge",
                                                    "weight": "medium"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "BOS",
                                                    "variant": "bodySmall",
                                                    "color": "#999999"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "end",
                                    "crossAlignment": "center",
                                    "padding": {
                                        "start": 0,
                                        "end": 0,
                                        "top": 4,
                                        "bottom": 0
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "PTS",
                                            "variant": "bodyMedium",
                                            "color": "#AAAAAA"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "32",
                                            "variant": "titleMedium",
                                            "weight": "bold",
                                            "color": "#FF6B6B"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "padding": {
                                "start": 0,
                                "end": 0,
                                "top": 6,
                                "bottom": 6
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/203507.png",
                                            "width": 40,
                                            "height": 40,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 20
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 12
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "LeBron James",
                                                    "variant": "bodyLarge",
                                                    "weight": "medium"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "LAL",
                                                    "variant": "bodySmall",
                                                    "color": "#999999"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "end",
                                    "crossAlignment": "center",
                                    "padding": {
                                        "start": 0,
                                        "end": 0,
                                        "top": 4,
                                        "bottom": 0
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "PTS",
                                            "variant": "bodyMedium",
                                            "color": "#AAAAAA"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "28",
                                            "variant": "titleMedium",
                                            "weight": "bold",
                                            "color": "#FF6B6B"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "padding": {
                                "start": 0,
                                "end": 0,
                                "top": 6,
                                "bottom": 6
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/203076.png",
                                            "width": 40,
                                            "height": 40,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 20
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 12
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Anthony Davis",
                                                    "variant": "bodyLarge",
                                                    "weight": "medium"
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "LAL",
                                                    "variant": "bodySmall",
                                                    "color": "#999999"
                                                }
                                            ]
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "alignment": "end",
                                    "crossAlignment": "center",
                                    "padding": {
                                        "start": 0,
                                        "end": 0,
                                        "top": 4,
                                        "bottom": 0
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "REB",
                                            "variant": "bodyMedium",
                                            "color": "#AAAAAA"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "14",
                                            "variant": "titleMedium",
                                            "weight": "bold",
                                            "color": "#FF6B6B"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        },
        {
            "id": "label-promobanner",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "PromoBanner",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-promo-banner",
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
                    "backgroundColor": "#17408B",
                    "cornerRadius": 12,
                    "padding": {
                        "start": 20,
                        "end": 20,
                        "top": 20,
                        "bottom": 20
                    },
                    "children": [
                        {
                            "type": "Image",
                            "src": "https://loremflickr.com/800/200/basketball,nba?lock=1",
                            "width": 64,
                            "height": 64,
                            "fit": "contain",
                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                            "cornerRadius": 8
                        },
                        {
                            "type": "Spacer",
                            "height": 16
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "children": [
                                {
                                    "type": "Text",
                                    "content": "WELCOME TO SDUI",
                                    "variant": "labelSmall",
                                    "weight": "bold",
                                    "color": "#FF6B6B"
                                },
                                {
                                    "type": "Spacer",
                                    "height": 4
                                },
                                {
                                    "type": "Text",
                                    "content": "All 20 semantic section types rendered from a single server response.",
                                    "variant": "bodySmall",
                                    "color": "#CCCCCC"
                                },
                                {
                                    "type": "Spacer",
                                    "height": 12
                                },
                                {
                                    "type": "Button",
                                    "label": "Learn More",
                                    "buttonVariant": "filled",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://scoreboard"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        },
        {
            "id": "label-heropanel",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "HeroPanel",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-content-card",
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
                        "start": 16,
                        "end": 16,
                        "top": 8,
                        "bottom": 8
                    },
                    "children": [
                        {
                            "type": "Container",
                            "direction": "column",
                            "cornerRadius": 12,
                            "backgroundColor": "#1A1F2E",
                            "actions": [
                                {
                                    "trigger": "onTap",
                                    "type": "navigate",
                                    "targetUri": "nba://article/celtics-lead-series"
                                }
                            ],
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://loremflickr.com/480/270/basketball,celtics?lock=2",
                                            "width": 280,
                                            "height": 140,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "padding": {
                                        "start": 12,
                                        "end": 12,
                                        "top": 12,
                                        "bottom": 12
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "ARTICLE",
                                            "variant": "labelSmall",
                                            "weight": "bold",
                                            "color": "#FF6B6B",
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 0,
                                                "bottom": 4
                                            }
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Celtics Lead Series 3-1",
                                            "variant": "titleSmall",
                                            "weight": "bold",
                                            "color": "#FFFFFF",
                                            "maxLines": 2
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Boston takes commanding lead after Game 4 victory",
                                            "variant": "bodySmall",
                                            "color": "#AAAAAA",
                                            "maxLines": 2,
                                            "padding": {
                                                "start": 0,
                                                "end": 0,
                                                "top": 4,
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
        },
        {
            "id": "label-contentrail",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "ContentRail",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-content-rail",
            "type": "AtomicComposite",
            "analyticsId": "demo_content_rail",
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
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Featured Content",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 8,
                                "bottom": 8
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": 12,
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-1",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-1"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "cornerRadius": 8,
                                            "backgroundColor": "#2A2A4A",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,highlights?lock=3",
                                                    "width": 200,
                                                    "height": 112,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": 4,
                                                        "end": 4,
                                                        "top": 4,
                                                        "bottom": 4
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "VIDEO",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#FF6B6B"
                                                        },
                                                        {
                                                            "type": "Spacer",
                                                            "height": 16
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Top 10 Plays",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "#FFFFFF",
                                            "maxLines": 2
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-2",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-2"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "cornerRadius": 8,
                                            "backgroundColor": "#2A2A4A",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,player?lock=4",
                                                    "width": 200,
                                                    "height": 112,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": 4,
                                                        "end": 4,
                                                        "top": 4,
                                                        "bottom": 4
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "ARTICLE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#FF6B6B"
                                                        },
                                                        {
                                                            "type": "Spacer",
                                                            "height": 16
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Player Spotlight",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "#FFFFFF",
                                            "maxLines": 2
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-3",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-3"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "cornerRadius": 8,
                                            "backgroundColor": "#2A2A4A",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,draft?lock=5",
                                                    "width": 200,
                                                    "height": 112,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": 4,
                                                        "end": 4,
                                                        "top": 4,
                                                        "bottom": 4
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "ARTICLE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#FF6B6B"
                                                        },
                                                        {
                                                            "type": "Spacer",
                                                            "height": 16
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Draft Preview",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "#FFFFFF",
                                            "maxLines": 2
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "rail-4",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://content/rail-4"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "cornerRadius": 8,
                                            "backgroundColor": "#2A2A4A",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,playoffs?lock=6",
                                                    "width": 200,
                                                    "height": 112,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": 4,
                                                        "end": 4,
                                                        "top": 4,
                                                        "bottom": 4
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "INTERACTIVE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#FF6B6B"
                                                        },
                                                        {
                                                            "type": "Spacer",
                                                            "height": 16
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Playoff Bracket",
                                            "variant": "bodySmall",
                                            "weight": "semiBold",
                                            "color": "#FFFFFF",
                                            "maxLines": 2
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        },
        {
            "id": "label-gamepanel",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "GamePanel",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-game-card",
            "type": "GamePanel",
            "analyticsId": "demo_game_card",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "gameId": "0022400888",
                "gameStatus": 3,
                "gameStatusText": "Final",
                "homeTeam": {
                    "teamId": 1610612744,
                    "teamTricode": "GSW",
                    "teamName": "Warriors",
                    "teamCity": "Golden State",
                    "score": 112,
                    "logoUrl": "https://cdn.nba.com/logos/nba/1610612744/primary/L/logo.svg"
                },
                "awayTeam": {
                    "teamId": 1610612745,
                    "teamTricode": "HOU",
                    "teamName": "Rockets",
                    "teamCity": "Houston",
                    "score": 105,
                    "logoUrl": "https://cdn.nba.com/logos/nba/1610612745/primary/L/logo.svg"
                },
                "actions": [
                    {
                        "trigger": "onTap",
                        "type": "navigate",
                        "targetUri": "nba://game/0022400888"
                    }
                ]
            }
        },
        {
            "id": "label-row",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Responsive Row (Container + breakpoint)",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-row",
            "type": "AtomicComposite",
            "analyticsId": "demo_row",
            "data": {
                "spacing": 16,
                "breakpoint": 600,
                "children": [
                    {
                        "id": "row-scoring-leader",
                        "type": "AtomicComposite",
                        "refreshPolicy": {
                            "type": "static"
                        },
                        "data": {
                            "ui": {
                                "type": "Container",
                                "direction": "column",
                                "padding": {
                                    "start": 16,
                                    "end": 16,
                                    "top": 8,
                                    "bottom": 8
                                },
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
                                            "bottom": 12
                                        }
                                    },
                                    {
                                        "type": "Container",
                                        "direction": "column",
                                        "padding": {
                                            "start": 0,
                                            "end": 0,
                                            "top": 6,
                                            "bottom": 6
                                        },
                                        "children": [
                                            {
                                                "type": "Container",
                                                "direction": "row",
                                                "crossAlignment": "center",
                                                "children": [
                                                    {
                                                        "type": "Image",
                                                        "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/203999.png",
                                                        "width": 40,
                                                        "height": 40,
                                                        "fit": "cover",
                                                        "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                                        "cornerRadius": 20
                                                    },
                                                    {
                                                        "type": "Spacer",
                                                        "height": 12
                                                    },
                                                    {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "Nikola Jokić",
                                                                "variant": "bodyLarge",
                                                                "weight": "medium"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "DEN",
                                                                "variant": "bodySmall",
                                                                "color": "#999999"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "Container",
                                                "direction": "row",
                                                "alignment": "end",
                                                "crossAlignment": "center",
                                                "padding": {
                                                    "start": 0,
                                                    "end": 0,
                                                    "top": 4,
                                                    "bottom": 0
                                                },
                                                "children": [
                                                    {
                                                        "type": "Text",
                                                        "content": "PTS",
                                                        "variant": "bodyMedium",
                                                        "color": "#AAAAAA"
                                                    },
                                                    {
                                                        "type": "Spacer",
                                                        "height": 8
                                                    },
                                                    {
                                                        "type": "Text",
                                                        "content": "26.4",
                                                        "variant": "titleMedium",
                                                        "weight": "bold",
                                                        "color": "#FF6B6B"
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
                        }
                    },
                    {
                        "id": "row-assists-leader",
                        "type": "AtomicComposite",
                        "refreshPolicy": {
                            "type": "static"
                        },
                        "data": {
                            "ui": {
                                "type": "Container",
                                "direction": "column",
                                "padding": {
                                    "start": 16,
                                    "end": 16,
                                    "top": 8,
                                    "bottom": 8
                                },
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
                                            "bottom": 12
                                        }
                                    },
                                    {
                                        "type": "Container",
                                        "direction": "column",
                                        "padding": {
                                            "start": 0,
                                            "end": 0,
                                            "top": 6,
                                            "bottom": 6
                                        },
                                        "children": [
                                            {
                                                "type": "Container",
                                                "direction": "row",
                                                "crossAlignment": "center",
                                                "children": [
                                                    {
                                                        "type": "Image",
                                                        "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/201566.png",
                                                        "width": 40,
                                                        "height": 40,
                                                        "fit": "cover",
                                                        "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                                        "cornerRadius": 20
                                                    },
                                                    {
                                                        "type": "Spacer",
                                                        "height": 12
                                                    },
                                                    {
                                                        "type": "Container",
                                                        "direction": "column",
                                                        "children": [
                                                            {
                                                                "type": "Text",
                                                                "content": "Trae Young",
                                                                "variant": "bodyLarge",
                                                                "weight": "medium"
                                                            },
                                                            {
                                                                "type": "Text",
                                                                "content": "ATL",
                                                                "variant": "bodySmall",
                                                                "color": "#999999"
                                                            }
                                                        ]
                                                    }
                                                ]
                                            },
                                            {
                                                "type": "Container",
                                                "direction": "row",
                                                "alignment": "end",
                                                "crossAlignment": "center",
                                                "padding": {
                                                    "start": 0,
                                                    "end": 0,
                                                    "top": 4,
                                                    "bottom": 0
                                                },
                                                "children": [
                                                    {
                                                        "type": "Text",
                                                        "content": "AST",
                                                        "variant": "bodyMedium",
                                                        "color": "#AAAAAA"
                                                    },
                                                    {
                                                        "type": "Spacer",
                                                        "height": 8
                                                    },
                                                    {
                                                        "type": "Text",
                                                        "content": "11.1",
                                                        "variant": "titleMedium",
                                                        "weight": "bold",
                                                        "color": "#FF6B6B"
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
        {
            "id": "label-tabgroup",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "TabGroup",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-tab-group",
            "type": "TabGroup",
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
                                        "start": 16,
                                        "end": 16,
                                        "top": 8,
                                        "bottom": 8
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "cornerRadius": 12,
                                            "backgroundColor": "#1A1F2E",
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "children": [
                                                        {
                                                            "type": "Image",
                                                            "src": "https://loremflickr.com/480/270/basketball,season?lock=7",
                                                            "width": 280,
                                                            "height": 140,
                                                            "fit": "cover",
                                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": 12,
                                                        "end": 12,
                                                        "top": 12,
                                                        "bottom": 12
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "ARTICLE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FF6B6B",
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": 4
                                                            }
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "Season Overview",
                                                            "variant": "titleSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "maxLines": 2
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "The 2024-25 season has been full of surprises",
                                                            "variant": "bodySmall",
                                                            "color": "#AAAAAA",
                                                            "maxLines": 2,
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 4,
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
                                        "start": 16,
                                        "end": 16,
                                        "top": 8,
                                        "bottom": 8
                                    },
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "cornerRadius": 12,
                                            "backgroundColor": "#1A1F2E",
                                            "children": [
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "children": [
                                                        {
                                                            "type": "Image",
                                                            "src": "https://loremflickr.com/480/270/basketball,stats?lock=8",
                                                            "width": 280,
                                                            "height": 140,
                                                            "fit": "cover",
                                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                        }
                                                    ]
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "column",
                                                    "padding": {
                                                        "start": 12,
                                                        "end": 12,
                                                        "top": 12,
                                                        "bottom": 12
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "INTERACTIVE",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FF6B6B",
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 0,
                                                                "bottom": 4
                                                            }
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "League Statistical Leaders",
                                                            "variant": "titleSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "maxLines": 2
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "Points, rebounds, assists and more",
                                                            "variant": "bodySmall",
                                                            "color": "#AAAAAA",
                                                            "maxLines": 2,
                                                            "padding": {
                                                                "start": 0,
                                                                "end": 0,
                                                                "top": 4,
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
            }
        },
        {
            "id": "label-boxscoretable",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "BoxscoreTable",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-boxscore-table",
            "type": "BoxscoreTable",
            "analyticsId": "demo_boxscore_table",
            "data": {
                "teamTricode": "BOS",
                "teamName": "Boston Celtics",
                "teamColor": "#007A33",
                "teamLogoUrl": "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg",
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
                        "imageUrl": "https://cdn.nba.com/headshots/nba/latest/1040x760/1628369.png",
                        "starter": true,
                        "stats": {
                            "ast": 5,
                            "reb": 8,
                            "min": "38:12",
                            "pts": 32,
                            "fgPct": ".545"
                        }
                    },
                    {
                        "playerId": "1627759",
                        "name": "J. Brown",
                        "position": "SG",
                        "jerseyNumber": "7",
                        "imageUrl": "https://cdn.nba.com/headshots/nba/latest/1040x760/1627759.png",
                        "starter": true,
                        "stats": {
                            "ast": 3,
                            "reb": 5,
                            "min": "36:45",
                            "pts": 26,
                            "fgPct": ".480"
                        }
                    },
                    {
                        "playerId": "1629684",
                        "name": "D. White",
                        "position": "PG",
                        "jerseyNumber": "9",
                        "imageUrl": "https://cdn.nba.com/headshots/nba/latest/1040x760/1629684.png",
                        "starter": false,
                        "stats": {
                            "ast": 6,
                            "reb": 4,
                            "min": "32:10",
                            "pts": 18,
                            "fgPct": ".500"
                        }
                    }
                ],
                "sortStateKey": "demo_boxscore_sortCol",
                "sortDirectionStateKey": "demo_boxscore_sortDir"
            }
        },
        {
            "id": "label-form",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Form",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-form",
            "type": "Form",
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
                    "target": "leaders-table",
                    "endpoint": "/sdui/refresh/stats-leaders",
                    "paramBindings": {
                        "season": "{{form_season}}",
                        "seasonType": "{{form_season_type}}",
                        "perMode": "{{form_per_mode}}",
                        "statCategory": "{{form_stat_category}}"
                    }
                },
                "submitLabel": "Search"
            }
        },
        {
            "id": "label-seasonleaderstable",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SeasonLeadersTable",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "leaders-table",
            "type": "SeasonLeadersTable",
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
            }
        },
        {
            "id": "label-gamepanel (featured)",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "GamePanel (featured)",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-featured-game-panel",
            "type": "GamePanel",
            "analyticsId": "demo_featured_game_panel",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "gameId": "0022400777",
                "gameStatus": 2,
                "gameStatusText": "Q4 2:15",
                "gameTimeEt": "2025-03-11T19:30:00-04:00",
                "variant": "featured",
                "backgroundImageUrl": "https://loremflickr.com/1200/600/basketball,arena?lock=9",
                "fallbackThumbnailUrl": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                "badgeText": "LIVE",
                "homeTeam": {
                    "teamId": 1610612748,
                    "teamTricode": "MIA",
                    "teamName": "Heat",
                    "teamCity": "Miami",
                    "score": 101,
                    "logoUrl": "https://cdn.nba.com/logos/nba/1610612748/primary/L/logo.svg"
                },
                "awayTeam": {
                    "teamId": 1610612751,
                    "teamTricode": "BKN",
                    "teamName": "Nets",
                    "teamCity": "Brooklyn",
                    "score": 97,
                    "logoUrl": "https://cdn.nba.com/logos/nba/1610612751/primary/L/logo.svg"
                },
                "actions": [
                    {
                        "trigger": "onTap",
                        "type": "navigate",
                        "targetUri": "nba://game/0022400777"
                    }
                ]
            }
        },
        {
            "id": "label-videocarousel",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "VideoCarousel",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-video-carousel",
            "type": "AtomicComposite",
            "analyticsId": "demo_video_carousel",
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
                        "top": 8,
                        "bottom": 8
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Top Highlights",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "#FFFFFF",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 4,
                                "bottom": 4
                            }
                        },
                        {
                            "type": "Text",
                            "content": "Today's best plays",
                            "variant": "bodySmall",
                            "color": "#AAAAAA",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 2,
                                "bottom": 2
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": 12,
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "id": "vid-1",
                                    "cornerRadius": 12,
                                    "backgroundColor": "#1A1F2E",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-1"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,pass?lock=10",
                                                    "width": 240,
                                                    "height": 135,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": 6,
                                                        "end": 6,
                                                        "top": 6,
                                                        "bottom": 6
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "NEW",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#C8102E"
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "1:24",
                                                            "variant": "labelSmall",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#000000B3"
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": 10,
                                                "end": 10,
                                                "top": 10,
                                                "bottom": 10
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Dončić No-Look Dime",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Lakers vs Celtics",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": 2,
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
                                    "cornerRadius": 12,
                                    "backgroundColor": "#1A1F2E",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-2"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,crossover?lock=11",
                                                    "width": 240,
                                                    "height": 135,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": 6,
                                                        "end": 6,
                                                        "top": 6,
                                                        "bottom": 6
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Spacer",
                                                            "height": 1
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "0:48",
                                                            "variant": "labelSmall",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#000000B3"
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": 10,
                                                "end": 10,
                                                "top": 10,
                                                "bottom": 10
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "SGA Crossover & Finish",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Thunder vs Nuggets",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": 2,
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
                                    "cornerRadius": 12,
                                    "backgroundColor": "#1A1F2E",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-3"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,dunk?lock=12",
                                                    "width": 240,
                                                    "height": 135,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": 6,
                                                        "end": 6,
                                                        "top": 6,
                                                        "bottom": 6
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Text",
                                                            "content": "NEW",
                                                            "variant": "labelSmall",
                                                            "weight": "bold",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#C8102E"
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "0:32",
                                                            "variant": "labelSmall",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#000000B3"
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": 10,
                                                "end": 10,
                                                "top": 10,
                                                "bottom": 10
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Edwards Poster Dunk",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Timberwolves vs Suns",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": 2,
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
                                    "cornerRadius": 12,
                                    "backgroundColor": "#1A1F2E",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://video/vid-4"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Image",
                                                    "src": "https://loremflickr.com/480/270/basketball,triple+double?lock=13",
                                                    "width": 240,
                                                    "height": 135,
                                                    "fit": "cover",
                                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"
                                                },
                                                {
                                                    "type": "Container",
                                                    "direction": "row",
                                                    "alignment": "spaceBetween",
                                                    "crossAlignment": "center",
                                                    "padding": {
                                                        "start": 6,
                                                        "end": 6,
                                                        "top": 6,
                                                        "bottom": 6
                                                    },
                                                    "children": [
                                                        {
                                                            "type": "Spacer",
                                                            "height": 1
                                                        },
                                                        {
                                                            "type": "Text",
                                                            "content": "3:15",
                                                            "variant": "labelSmall",
                                                            "color": "#FFFFFF",
                                                            "backgroundColor": "#000000B3"
                                                        }
                                                    ]
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "padding": {
                                                "start": 10,
                                                "end": 10,
                                                "top": 10,
                                                "bottom": 10
                                            },
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "Jokić Triple-Double Recap",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 2
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Full highlights",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
                                                    "maxLines": 1,
                                                    "padding": {
                                                        "start": 0,
                                                        "end": 0,
                                                        "top": 2,
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
            }
        },
        {
            "id": "label-nbatvschedule",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "NbaTvSchedule",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-nbatv-schedule",
            "type": "AtomicComposite",
            "analyticsId": "demo_nbatv_schedule",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "children": [
                        {
                            "type": "Container",
                            "direction": "column",
                            "alignment": "end",
                            "cornerRadius": 12,
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 8,
                                "bottom": 8
                            },
                            "children": [
                                {
                                    "type": "Image",
                                    "src": "https://loremflickr.com/800/400/basketball,arena?lock=14",
                                    "height": 200,
                                    "fit": "cover",
                                    "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                    "fillWidth": true,
                                    "cornerRadius": 12
                                },
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "padding": {
                                        "start": 16,
                                        "end": 16,
                                        "top": 16,
                                        "bottom": 16
                                    },
                                    "backgroundGradient": {
                                        "colors": [
                                            "#00000000",
                                            "#000000CC"
                                        ],
                                        "direction": "vertical"
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "LIVE",
                                            "variant": "labelSmall",
                                            "weight": "bold",
                                            "color": "#FFFFFF",
                                            "backgroundColor": "#C8102E"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 6
                                        },
                                        {
                                            "type": "Text",
                                            "content": "NBA TV Live",
                                            "variant": "titleLarge",
                                            "weight": "bold",
                                            "color": "#FFFFFF"
                                        },
                                        {
                                            "type": "Text",
                                            "content": "Lakers vs Celtics — Coverage begins at 7:00 PM ET",
                                            "variant": "bodyMedium",
                                            "color": "#CCCCCC"
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            "type": "Spacer",
                            "height": 8
                        },
                        {
                            "type": "Text",
                            "content": "Today's Schedule",
                            "variant": "titleSmall",
                            "weight": "bold",
                            "color": "#FFFFFF",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 4,
                                "bottom": 4
                            }
                        },
                        {
                            "type": "Container",
                            "direction": "column",
                            "gap": 8,
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 0,
                                "bottom": 0
                            },
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "id": "slot-1",
                                    "fillWidth": true,
                                    "cornerRadius": 8,
                                    "backgroundColor": "#1A1F2E",
                                    "padding": {
                                        "start": 12,
                                        "end": 12,
                                        "top": 12,
                                        "bottom": 12
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "18:00",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "#999999"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 12
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "NBA GameTime",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Pre-game analysis and predictions",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
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
                                    "fillWidth": true,
                                    "cornerRadius": 8,
                                    "backgroundColor": "#1A1F2E",
                                    "padding": {
                                        "start": 12,
                                        "end": 12,
                                        "top": 12,
                                        "bottom": 12
                                    },
                                    "actions": [
                                        {
                                            "trigger": "onTap",
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
                                            "color": "#999999"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 12
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "LAL @ BOS",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Live game broadcast",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
                                                    "maxLines": 1
                                                }
                                            ]
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 8
                                        },
                                        {
                                            "type": "Text",
                                            "content": "LIVE",
                                            "variant": "labelSmall",
                                            "weight": "bold",
                                            "color": "#FFFFFF",
                                            "backgroundColor": "#C8102E"
                                        }
                                    ]
                                },
                                {
                                    "type": "Container",
                                    "direction": "row",
                                    "crossAlignment": "center",
                                    "id": "slot-3",
                                    "fillWidth": true,
                                    "cornerRadius": 8,
                                    "backgroundColor": "#1A1F2E",
                                    "padding": {
                                        "start": 12,
                                        "end": 12,
                                        "top": 12,
                                        "bottom": 12
                                    },
                                    "children": [
                                        {
                                            "type": "Text",
                                            "content": "22:00",
                                            "variant": "bodyMedium",
                                            "weight": "semiBold",
                                            "color": "#999999"
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 12
                                        },
                                        {
                                            "type": "Container",
                                            "direction": "column",
                                            "children": [
                                                {
                                                    "type": "Text",
                                                    "content": "NBA Inside Stuff",
                                                    "variant": "bodyMedium",
                                                    "weight": "semiBold",
                                                    "color": "#FFFFFF",
                                                    "maxLines": 1
                                                },
                                                {
                                                    "type": "Text",
                                                    "content": "Post-game interviews and highlights",
                                                    "variant": "bodySmall",
                                                    "color": "#999999",
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
            }
        },
        {
            "id": "label-subscribebanner",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SubscribeBanner",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-subscribe-banner",
            "type": "SubscribeBanner",
            "analyticsId": "demo_subscribe_banner",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "title": "Never Miss a Game",
                "subtitle": "Stream every out-of-market game live with NBA League Pass.",
                "backgroundImageUrl": "https://loremflickr.com/800/200/basketball,court?lock=18",
                "logoUrl": "https://cdn.nba.com/manage/2025/01/league-pass-logo.png",
                "fallbackThumbnailUrl": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                "ctaLabel": "Subscribe Now",
                "ctaAction": {
                    "trigger": "onTap",
                    "type": "navigate",
                    "targetUri": "nba://subscribe",
                    "presentation": "modal"
                }
            }
        },
        {
            "id": "label-subscribehero",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SubscribeHero",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-subscribe-hero",
            "type": "SubscribeHero",
            "analyticsId": "demo_subscribe_hero",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "title": "NBA League Pass",
                "subtitle": "Watch every game. Your way.",
                "backgroundImageUrl": "https://loremflickr.com/1200/600/basketball,court?lock=19",
                "logoUrl": "https://cdn.nba.com/manage/2025/01/league-pass-logo.png",
                "fallbackThumbnailUrl": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                "features": [
                    "Live & on-demand out-of-market games",
                    "Multiple viewing angles and condensed replays",
                    "NBA TV included",
                    "Compatible with all major devices"
                ],
                "tiers": [
                    {
                        "id": "tier-standard",
                        "name": "League Pass",
                        "price": "$14.99/mo",
                        "originalPrice": "$22.99/mo",
                        "badgeText": "MOST POPULAR",
                        "features": [
                            "All out-of-market games",
                            "3 concurrent streams",
                            "HD quality"
                        ],
                        "ctaLabel": "Start Free Trial",
                        "ctaAction": {
                            "trigger": "onTap",
                            "type": "navigate",
                            "targetUri": "nba://subscribe/standard"
                        }
                    },
                    {
                        "id": "tier-premium",
                        "name": "League Pass Premium",
                        "price": "$22.99/mo",
                        "badgeText": "BEST VALUE",
                        "features": [
                            "Everything in League Pass",
                            "No ads on VOD",
                            "Unlimited concurrent streams",
                            "In-arena camera angles"
                        ],
                        "ctaLabel": "Go Premium",
                        "ctaAction": {
                            "trigger": "onTap",
                            "type": "navigate",
                            "targetUri": "nba://subscribe/premium"
                        }
                    }
                ]
            }
        },
        {
            "id": "label-followingrail",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "FollowingRail",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-following-rail",
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
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Following",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 4,
                                "bottom": 4
                            }
                        },
                        {
                            "type": "ScrollContainer",
                            "direction": "row",
                            "gap": 16,
                            "children": [
                                {
                                    "type": "Container",
                                    "direction": "column",
                                    "alignment": "center",
                                    "crossAlignment": "center",
                                    "id": "team-lal",
                                    "actions": [
                                        {
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://team/1610612747"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 28
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
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
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://team/1610612738"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 28
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
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
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://player/203999"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/203999.png",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 28
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
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
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://team/1610612744"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/logos/nba/1610612744/primary/L/logo.svg",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 28
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
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
                                            "trigger": "onTap",
                                            "type": "navigate",
                                            "targetUri": "nba://player/1630175"
                                        }
                                    ],
                                    "children": [
                                        {
                                            "type": "Image",
                                            "src": "https://cdn.nba.com/headshots/nba/latest/1040x760/1630175.png",
                                            "width": 56,
                                            "height": 56,
                                            "fit": "cover",
                                            "placeholder": "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png",
                                            "cornerRadius": 28
                                        },
                                        {
                                            "type": "Spacer",
                                            "height": 4
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
            }
        },
        {
            "id": "label-displaygrid",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "DisplayGrid",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-display-grid",
            "type": "AtomicComposite",
            "analyticsId": "demo_display_grid",
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
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "Eastern Conference Standings",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "padding": {
                                "start": 16,
                                "end": 16,
                                "top": 8,
                                "bottom": 8
                            }
                        },
                        {
                            "type": "DisplayGrid",
                            "id": "demo-display-grid-grid",
                            "headerVariant": "labelMedium",
                            "cellVariant": "bodySmall",
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
            }
        },
        {
            "id": "label-errorstate",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "ErrorState",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-error-state",
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
                        "start": 16,
                        "end": 16,
                        "top": 32,
                        "bottom": 32
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "⚠️",
                            "variant": "titleLarge"
                        },
                        {
                            "type": "Spacer",
                            "height": 12
                        },
                        {
                            "type": "Text",
                            "content": "Something went wrong",
                            "variant": "titleMedium",
                            "weight": "bold"
                        },
                        {
                            "type": "Spacer",
                            "height": 8
                        },
                        {
                            "type": "Text",
                            "content": "We couldn't load this content. This is a demo of the ErrorState section type.",
                            "variant": "bodyMedium",
                            "color": "#888888"
                        },
                        {
                            "type": "Spacer",
                            "height": 16
                        },
                        {
                            "type": "Button",
                            "label": "Try Again",
                            "buttonVariant": "filled",
                            "actions": [
                                {
                                    "trigger": "onTap",
                                    "type": "navigate",
                                    "targetUri": "nba://scoreboard"
                                }
                            ]
                        }
                    ]
                }
            }
        },
        {
            "id": "label-sectionslot (adslot in atomic tree)",
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
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "SectionSlot (AdSlot in atomic tree)",
                            "variant": "titleMedium",
                            "weight": "bold"
                        }
                    ]
                }
            }
        },
        {
            "id": "demo-section-slot",
            "type": "AtomicComposite",
            "analyticsId": "demo-section-slot",
            "refreshPolicy": {
                "type": "static"
            },
            "data": {
                "ui": {
                    "type": "Container",
                    "direction": "column",
                    "backgroundColor": "#1A1A2E",
                    "cornerRadius": 12,
                    "padding": {
                        "start": 16,
                        "end": 16,
                        "top": 12,
                        "bottom": 12
                    },
                    "children": [
                        {
                            "type": "Text",
                            "content": "LAL vs BOS",
                            "variant": "titleMedium",
                            "weight": "bold",
                            "color": "#FFFFFF"
                        },
                        {
                            "type": "Text",
                            "content": "Q3 5:42 • LAL 87 - BOS 82",
                            "variant": "bodySmall",
                            "color": "#7a8baa"
                        },
                        {
                            "type": "Divider",
                            "color": "#333333",
                            "thickness": 1
                        },
                        {
                            "type": "SectionSlot",
                            "id": "inline-ad",
                            "section": {
                                "id": "demo-inline-ad",
                                "type": "AdSlot",
                                "data": {
                                    "adUnitPath": "/nba/game-card-inline",
                                    "size": "banner"
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
            }
        }
    ]
}
```