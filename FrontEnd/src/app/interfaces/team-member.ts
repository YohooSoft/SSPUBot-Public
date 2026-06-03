export interface TeamMember {
    avatar?: string;
    name: string;
    description: string;
    link: string | null;
    type: AvatarSourceType,
    githubUsername?: string;
}
